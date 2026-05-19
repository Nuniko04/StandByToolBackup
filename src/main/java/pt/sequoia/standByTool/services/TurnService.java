package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public TurnService(TurnRepository turnRepository, CalendarService calendarService,
                       UserRepository userRepository, TurnTypeRepository turnTypeRepository,
                       AuditLogService auditLogService) {
        this.turnRepository = turnRepository;
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.turnTypeRepository = turnTypeRepository;
        this.auditLogService = auditLogService;
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
        return turnRepository.findAll().stream()
                .filter(t -> t.getAssignee().getId().equals(userId))
                .toList();
    }

    @Transactional
    public Turn createManualTurn(UUID assigneeId, UUID turnTypeId, LocalDateTime start, LocalDateTime end, User assigner) {
        User assignee = userRepository.findById(assigneeId).orElseThrow();
        TurnType type = turnTypeRepository.findById(turnTypeId).orElseThrow();

        // 💡 1. UTILIZAÇÃO DA MATRIZ DE ELEGIBILIDADE (Colocada no topo e protegida contra nulos)
        if (assignee.getEligibleTurnTypes() == null || assignee.getEligibleTurnTypes().stream().noneMatch(tt -> tt.getId().equals(type.getId()))) {
            throw new IllegalArgumentException("Erro: O colaborador " + assignee.getName() + " não tem permissão/elegibilidade para realizar turnos do tipo " + type.getName() + ".");
        }

        // 🛡️ 2. A BARREIRA DE SEGURANÇA CONTRA SOBREPOSIÇÕES 🛡️
        boolean jaTemTurno = turnRepository.existsByAssigneeAndDates(assigneeId, start, end);
        if (jaTemTurno) {
            throw new IllegalArgumentException("O colaborador " + assignee.getName() + " já tem um turno atribuído que se sobrepõe a estas datas.");
        }

        // ⚙️ 3. CRIAÇÃO E PERSISTÊNCIA
        Turn turn = new Turn();
        turn.setAssignee(assignee);
        turn.setTurnType(type);
        turn.setStartTime(start);
        turn.setEndTime(end);
        turn.setTurnValue(type.getDefaultValue());
        turn.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
        turn.setCreatedBy(assigner);

        Turn saved = turnRepository.save(turn);
        auditLogService.log(assigner, "CREATE_MANUAL_TURN", "Turn", saved.getId(), "Manual assignment created");
        return saved;
    }

    @Transactional
    public boolean updateTurn(UUID turnId, UUID newAssigneeId, LocalDateTime newStart, LocalDateTime newEnd, User assigner) {
        Optional<Turn> opt = turnRepository.findById(turnId);
        if (opt.isPresent()) {
            Turn turn = opt.get();

            // 1. Variáveis finais para a matemática de conflitos e elegibilidade
            UUID finalAssigneeId = newAssigneeId != null ? newAssigneeId : turn.getAssignee().getId();
            LocalDateTime finalStart = newStart != null ? newStart : turn.getStartTime();
            LocalDateTime finalEnd = newEnd != null ? newEnd : turn.getEndTime();

            // 💡 UTILIZAÇÃO DA MATRIZ DE ELEGIBILIDADE 💡
            // Buscamos o colaborador que ficará com o turno (atual ou o novo)
            User colaboradorAlvo = userRepository.findById(finalAssigneeId)
                    .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado."));

            // Verificamos se o tipo deste turno está presente na lista de elegibilidade do colaborador
            boolean éElegivel = colaboradorAlvo.getEligibleTurnTypes().stream()
                    .anyMatch(tt -> tt.getId().equals(turn.getTurnType().getId()));

            if (!éElegivel) {
                throw new IllegalArgumentException("Edição bloqueada: O colaborador " + colaboradorAlvo.getName() +
                        " não tem elegibilidade/permissão para realizar turnos do tipo " + turn.getTurnType().getName() + ".");
            }

            // 🛡️ A BARREIRA DE SEGURANÇA CONTRA SOBREPOSIÇÕES CONTRA OUTROS TURNOS 🛡️
            List<Turn> outrosTurnos = turnRepository.findAll().stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(finalAssigneeId))
                    .filter(t -> !t.getId().equals(turnId)) // Ignora o próprio turno em edição
                    .toList();

            boolean temConflito = outrosTurnos.stream().anyMatch(t ->
                    !(finalEnd.isBefore(t.getStartTime()) || finalStart.isAfter(t.getEndTime()))
            );

            if (temConflito) {
                throw new IllegalArgumentException("Edição bloqueada: As novas datas entram em conflito com outro turno já atribuído a este colaborador.");
            }

            StringBuilder changes = new StringBuilder();
            boolean mudouDePessoa = false;

            // 2. Aplicar mudança de utilizador (Se existir)
            if (newAssigneeId != null && !newAssigneeId.equals(turn.getAssignee().getId())) {
                // Como já buscámos e validámos o colaboradorAlvo acima, escusamos de ir à DB outra vez:
                changes.append("Assignee: ").append(turn.getAssignee().getName()).append(" -> ").append(colaboradorAlvo.getName()).append("; ");

                // MUDANÇA DE PESSOA: APAGA O EVENTO DO CALENDÁRIO!
                if (turn.getCalendarEventId() != null && !turn.getCalendarEventId().isBlank()) {
                    calendarService.deleteTurnFromCalendar(turn);
                    turn.setCalendarEventId(null); // Limpa a matrícula
                }

                turn.setAssignee(colaboradorAlvo);
                turn.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE); // Força a nova aceitação
                mudouDePessoa = true;
            }

            // 3. Aplicar mudança de Datas
            if (newStart != null && !newStart.equals(turn.getStartTime())) {
                changes.append("Start: ").append(turn.getStartTime()).append(" -> ").append(newStart).append("; ");
                turn.setStartTime(newStart);
            }
            if (newEnd != null && !newEnd.equals(turn.getEndTime())) {
                changes.append("End: ").append(turn.getEndTime()).append(" -> ").append(newEnd).append("; ");
                turn.setEndTime(newEnd);
            }

            // 4. ATUALIZAR CALENDÁRIO CENTRAL (Apenas se manteve a mesma pessoa!)
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
}