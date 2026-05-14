package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Cartao;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TurnService {

    private final TurnRepository turnRepository;
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final TurnTypeRepository turnTypeRepository;
    private final CartaoRepository cartaoRepository;
    private final AuditLogService auditLogService;

    public TurnService(TurnRepository turnRepository, CalendarService calendarService,
                       UserRepository userRepository, TurnTypeRepository turnTypeRepository,
                       CartaoRepository cartaoRepository, AuditLogService auditLogService) {
        this.turnRepository = turnRepository;
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.turnTypeRepository = turnTypeRepository;
        this.cartaoRepository = cartaoRepository;
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
    public Turn createManualTurn(UUID assigneeId, UUID turnTypeId, LocalDate start, LocalDate end, User assigner) {
        User assignee = userRepository.findById(assigneeId).orElseThrow();
        TurnType type = turnTypeRepository.findById(turnTypeId).orElseThrow();

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
    public boolean updateTurn(UUID turnId, LocalDate newStart, LocalDate newEnd, Long cartaoId, User assigner) {
        Optional<Turn> opt = turnRepository.findById(turnId);
        if (opt.isPresent()) {
            Turn turn = opt.get();
            StringBuilder changes = new StringBuilder();

            if (newStart != null) {
                changes.append("Start: ").append(turn.getStartTime()).append(" -> ").append(newStart).append("; ");
                turn.setStartTime(newStart);
            }
            if (newEnd != null) {
                changes.append("End: ").append(turn.getEndTime()).append(" -> ").append(newEnd).append("; ");
                turn.setEndTime(newEnd);
            }
            if (cartaoId != null) {
                Cartao cartao = cartaoRepository.findById(cartaoId).orElse(null);
                turn.setCartao(cartao);
                turn.setDataEntregaCartao(LocalDate.now());
                changes.append("Card assigned: ").append(cartaoId);
            }

            if (turn.getCalendarEventId() != null) {
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