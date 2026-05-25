package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Notification;
import pt.sequoia.standByTool.models.Request;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.models.enums.RequestType;
import pt.sequoia.standByTool.repositories.NotificationRepository;
import pt.sequoia.standByTool.repositories.RequestRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final TurnRepository turnRepository;
    private final UserRepository userRepository;
    private final CalendarService calendarService;
    private final AuditLogService auditLogService;
    public final NotificationService notificationService;

    public RequestService(RequestRepository requestRepository, TurnRepository turnRepository,
                          UserRepository userRepository, CalendarService calendarService, AuditLogService auditLogService, NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.turnRepository = turnRepository;
        this.userRepository = userRepository;
        this.calendarService = calendarService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    public List<Request> getPendingRequests() {
        // Num cenário real poderíamos fazer uma query no repository: findByStatus(PENDING)
        return requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .toList();
    }

    public List<Request> getHistoryRequests() {
        return requestRepository.findByStatusNotOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    public List<Request> getRequestsByUser(UUID userId) {
        return requestRepository.findAll().stream()
                .filter(r -> r.getRequester().getId().equals(userId))
                .toList();
    }

    // 1. Colaborador pede Férias
    @Transactional
    public Request createTimeOffRequest(UUID userId, LocalDate start, LocalDate end, String note) {
        User requester = userRepository.findById(userId).orElseThrow();

        Request request = new Request();
        request.setRequestType(RequestType.TIME_OFF);
        request.setRequester(requester);
        request.setTimeOffStart(start);
        request.setTimeOffEnd(end);
        request.setRequesterNote(note);

        return requestRepository.save(request);
    }

    // 2. Colaborador pede para Trocar um Turno
    @Transactional
    public Request createSwapRequest(UUID userId, UUID turnId, UUID targetUserId, String note) {
        User requester = userRepository.findById(userId).orElseThrow();
        Turn turn = turnRepository.findById(turnId).orElseThrow();
        User targetUser = userRepository.findById(targetUserId).orElseThrow();

        // 💡 1. BLOQUEAR O TURNO: Muda o estado para SWAP_REQUESTED
        turn.setTurnStatus(pt.sequoia.standByTool.models.enums.TurnStatus.SWAP_REQUESTED);
        turnRepository.save(turn); // Guarda a alteração do turno na base de dados

        Request request = new Request();
        request.setRequestType(RequestType.TURN_SWAP);
        request.setRequester(requester);
        request.setTargetUser(targetUser);
        request.setTurn(turn);
        request.setRequesterNote(note);

        return requestRepository.save(request);
    }

    @Transactional
    public boolean processRequest(UUID requestId, UUID assignerId, RequestStatus status, String assignerNote, UUID newAssigneeId) {
        Optional<Request> opt = requestRepository.findById(requestId);

        if (opt.isPresent()) {
            Request request = opt.get();
            User assigner = userRepository.findById(assignerId).orElseThrow();

            request.setStatus(status);
            request.setProcessedBy(assigner);
            request.setAssignerNote(assignerNote);

            String logDetails = "Request processed as " + status;
            Turn turnoToSwap = request.getTurn();

            if (request.getRequestType() == RequestType.TURN_SWAP) {
                if (status == RequestStatus.APPROVED) {
                    // 💡 2. APROVADO: Passa para o novo utilizador e volta a ficar ACCEPTED
                    User novoColaborador = request.getTargetUser();
                    logDetails += ". Turn reassigned from " + turnoToSwap.getAssignee().getName() + " to " + novoColaborador.getName();

                    turnoToSwap.setAssignee(novoColaborador);
                    turnoToSwap.setTurnStatus(pt.sequoia.standByTool.models.enums.TurnStatus.ACCEPTED);

                    calendarService.updateTurnInCalendar(turnoToSwap);
                } else if (status == RequestStatus.DENIED) { // Nota: Lembra-te que no backend a palavra é DENIED
                    // 💡 O TEU NOVO FLUXO: Reverte para PENDING para forçar o dono original a re-confirmar
                    turnoToSwap.setTurnStatus(pt.sequoia.standByTool.models.enums.TurnStatus.PENDING_ACCEPTANCE);
                }

                turnRepository.save(turnoToSwap); // Guarda o turno com o novo estado

                // Sistema de Notificações
                String acao = status == RequestStatus.APPROVED ? "APROVADO ✅" : "REJEITADO ❌";
                String dataTurno = turnoToSwap.getStartTime().toLocalDate().toString();

                String msgA = "O teu pedido de troca de turno (" + dataTurno + ") com o/a " + request.getTargetUser().getName() + " foi " + acao + " pelos RH.";
                notificationService.criarNotificacao(request.getRequester(), msgA);

                String msgB = "A troca de turno (" + dataTurno + ") pedida pelo/a " + request.getRequester().getName() + " para ti foi " + acao + " pelos RH.";
                notificationService.criarNotificacao(request.getTargetUser(), msgB);
            }

            requestRepository.save(request);
            auditLogService.log(assigner, "PROCESS_REQUEST", "Request", requestId, logDetails);
            return true;
        }
        return false;
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }
}