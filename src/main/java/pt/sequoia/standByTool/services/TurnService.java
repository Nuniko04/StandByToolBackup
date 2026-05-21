package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Card;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TurnService {

    private final TurnRepository turnRepository;
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final TurnTypeRepository turnTypeRepository;
    private final AuditLogService auditLogService;
    private final CardRepository cardRepository;

    public TurnService(TurnRepository turnRepository, CalendarService calendarService,
                       UserRepository userRepository, TurnTypeRepository turnTypeRepository,
                       AuditLogService auditLogService, CardRepository cardRepository) {
        this.turnRepository = turnRepository;
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.turnTypeRepository = turnTypeRepository;
        this.auditLogService = auditLogService;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public boolean acceptTurn(UUID turnId, User loggedUser) {
        Optional<Turn> turnOpt = turnRepository.findById(turnId);
        if (turnOpt.isPresent()) {
            Turn turn = turnOpt.get();
            turn.setTurnStatus(TurnStatus.ACCEPTED);

            String eventId = calendarService.addTurnToCalendar(turn);
            if (eventId != null) {
                turn.setCalendarEventId(eventId);
            }
            turnRepository.save(turn);

            auditLogService.log(loggedUser, "ACCEPT_TURN", "Turn", turnId, "Status changed to ACCEPTED");
            return true;
        }
        return false;
    }

    public List<Turn> getAllTurns() {
        return turnRepository.findAll();
    }

    public List<Turn> getMyTurns(UUID userId) {
        return turnRepository.findByAssigneeIdOrderByStartTimeAscCreatedAtAsc(userId);
    }

    @Transactional
    public Turn createManualTurn(UUID assigneeId, UUID turnTypeId, UUID cardId, LocalDateTime start, LocalDateTime end, User assigner) {
        User assignee = userRepository.findById(assigneeId).orElseThrow();
        TurnType type = turnTypeRepository.findById(turnTypeId).orElseThrow();

        if (assignee.getEligibleTurnTypes() == null || assignee.getEligibleTurnTypes().stream().noneMatch(tt -> tt.getId().equals(type.getId()))) {
            throw new IllegalArgumentException("Erro: O colaborador " + assignee.getName() + " não tem permissão/elegibilidade para realizar turnos do tipo " + type.getName() + ".");
        }

        boolean jaTemTurno = turnRepository.existsByAssigneeAndDates(assigneeId, start, end);
        if (jaTemTurno) {
            throw new IllegalArgumentException("O colaborador " + assignee.getName() + " já tem um turno atribuído que se sobrepõe a estas datas.");
        }

        Turn turn = new Turn();
        turn.setAssignee(assignee);
        turn.setTurnType(type);
        turn.setStartTime(start);
        turn.setEndTime(end);
        turn.setTurnValue(type.getDefaultValue());
        turn.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
        turn.setCreatedBy(assigner);

        // 🛡️ VALIDAÇÃO E ATRIBUIÇÃO DO CARTÃO 🛡️
        if (cardId != null) {
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));

            // Usamos um UUID random falso na validação de criação porque o turno ainda não tem ID
            int conflitosCartao = turnRepository.countOverlappingTurnsWithCard(cardId, UUID.randomUUID(), start, end);
            if (conflitosCartao > 0) {
                throw new IllegalArgumentException("Atenção: O cartão selecionado já está a ser utilizado noutro turno durante estas datas!");
            }
            turn.setPaymentCard(card);
        }

        Turn saved = turnRepository.save(turn);
        auditLogService.log(assigner, "CREATE_MANUAL_TURN", "Turn", saved.getId(), "Manual assignment created");
        return saved;
    }

    @Transactional
    public boolean updateTurn(UUID turnId, UUID newAssigneeId, UUID newCardId, LocalDateTime newStart, LocalDateTime newEnd, User assigner) {
        Optional<Turn> opt = turnRepository.findById(turnId);
        if (opt.isPresent()) {
            Turn turn = opt.get();

            UUID finalAssigneeId = newAssigneeId != null ? newAssigneeId : turn.getAssignee().getId();
            LocalDateTime finalStart = newStart != null ? newStart : turn.getStartTime();
            LocalDateTime finalEnd = newEnd != null ? newEnd : turn.getEndTime();

            User colaboradorAlvo = userRepository.findById(finalAssigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado."));

            boolean éElegivel = colaboradorAlvo.getEligibleTurnTypes().stream()
                    .anyMatch(tt -> tt.getId().equals(turn.getTurnType().getId()));

            if (!éElegivel) {
                throw new IllegalArgumentException("Edição bloqueada: O colaborador " + colaboradorAlvo.getName() +
                        " não tem elegibilidade para realizar turnos do tipo " + turn.getTurnType().getName() + ".");
            }

            List<Turn> outrosTurnos = turnRepository.findAll().stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(finalAssigneeId))
                    .filter(t -> !t.getId().equals(turnId))
                    .toList();

            boolean temConflito = outrosTurnos.stream().anyMatch(t ->
                    !(finalEnd.isBefore(t.getStartTime()) || finalStart.isAfter(t.getEndTime()))
            );

            if (temConflito) {
                throw new IllegalArgumentException("Edição bloqueada: As novas datas entram em conflito com outro turno já atribuído a este colaborador.");
            }

            StringBuilder changes = new StringBuilder();
            boolean mudouDePessoa = false;

            if (newAssigneeId != null && !newAssigneeId.equals(turn.getAssignee().getId())) {
                changes.append("Assignee: ").append(turn.getAssignee().getName()).append(" -> ").append(colaboradorAlvo.getName()).append("; ");

                if (turn.getCalendarEventId() != null && !turn.getCalendarEventId().isBlank()) {
                    calendarService.deleteTurnFromCalendar(turn);
                    turn.setCalendarEventId(null);
                }

                turn.setAssignee(colaboradorAlvo);
                turn.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                mudouDePessoa = true;
            }

            if (newStart != null && !newStart.equals(turn.getStartTime())) {
                changes.append("Start: ").append(turn.getStartTime()).append(" -> ").append(newStart).append("; ");
                turn.setStartTime(newStart);
            }
            if (newEnd != null && !newEnd.equals(turn.getEndTime())) {
                changes.append("End: ").append(turn.getEndTime()).append(" -> ").append(newEnd).append("; ");
                turn.setEndTime(newEnd);
            }

            // 🛡️ ATUALIZAÇÃO E VALIDAÇÃO DO CARTÃO 🛡️
            if (newCardId != null) {
                Card card = cardRepository.findById(newCardId)
                        .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));

                int conflitosCartao = turnRepository.countOverlappingTurnsWithCard(newCardId, turnId, finalStart, finalEnd);
                if (conflitosCartao > 0) {
                    throw new IllegalArgumentException("Atenção: O cartão selecionado já está a ser utilizado noutro turno durante estas datas!");
                }

                turn.setPaymentCard(card);
                changes.append("Card updated; ");
            }

            if (!mudouDePessoa && turn.getCalendarEventId() != null && !turn.getCalendarEventId().isBlank()) {
                calendarService.updateTurnInCalendar(turn);
            }

            turnRepository.save(turn);
            auditLogService.log(assigner, "UPDATE_TURN", "Turn", turnId, changes.toString());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteTurn(UUID turnId, User assigner) {
        if (turnRepository.existsById(turnId)) {
            turnRepository.deleteById(turnId);
            auditLogService.log(assigner, "DELETE_TURN", "Turn", turnId, "Turn deleted");
            return true;
        }
        return false;
    }

    public Optional<Turn> getTurn(UUID id) {
        return turnRepository.findById(id);
    }

    @Transactional
    public int acceptAllPendingTurns(User loggedUser) {
        List<Turn> pendingTurns = turnRepository.findAll().stream()
                .filter(t -> t.getAssignee().getId().equals(loggedUser.getId())
                        && t.getTurnStatus() == TurnStatus.PENDING_ACCEPTANCE)
                .toList();

        int count = 0;
        for (Turn turn : pendingTurns) {
            turn.setTurnStatus(TurnStatus.ACCEPTED);

            String eventId = calendarService.addTurnToCalendar(turn);
            if (eventId != null) {
                turn.setCalendarEventId(eventId);
            }

            turnRepository.save(turn);
            count++;
        }

        if (count > 0) {
            auditLogService.log(loggedUser, "ACCEPT_ALL_TURNS", "Turn", loggedUser.getId(), "Accepted " + count + " pending turns");
        }

        return count; // Retorna quantos turnos foram aceites
    }
}