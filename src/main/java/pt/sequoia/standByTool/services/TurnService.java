package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Request;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.models.enums.RequestType;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.RequestRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.util.UUID;

@Service
public class TurnService {

    private final TurnRepository turnRepository;
    private final RequestRepository requestRepository;
    private final CalendarService calendarService;

    public TurnService(TurnRepository turnRepository, CalendarService calendarService, RequestRepository requestRepository) {
        this.turnRepository = turnRepository;
        this.requestRepository = requestRepository;
        this.calendarService = calendarService;
    }

    /**
     * Altera o estado do turno para ACCEPTED.
     */
    @Transactional
    public void acceptTurn(UUID turnId) {
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));

        if (turn.getTurnStatus() != TurnStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Apenas turnos pendentes podem ser aceites.");
        }

        turn.setTurnStatus(TurnStatus.ACCEPTED);

        // Sincroniza com a Google
        String eventId = calendarService.addTurnToCalendar(turn);
        if (eventId != null) {
            turn.setCalendarEventId(eventId);
            System.out.println("✅ Evento adicionado ao Calendário: " + eventId);
        }

        turnRepository.save(turn);
    }

    /**
     * Cria um pedido de troca (TURN_SWAP) e marca o turno como SWAP_REQUESTED.
     */
    @Transactional
    public void requestSwap(UUID turnId, String note) {
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));

        // Cria o novo pedido de troca
        Request swapRequest = new Request();
        swapRequest.setRequestType(RequestType.TURN_SWAP); //
        swapRequest.setRequester(turn.getAssignee()); // O atual dono do turno é quem pede a troca
        swapRequest.setTurn(turn);
        swapRequest.setStatus(RequestStatus.PENDING); //
        swapRequest.setRequesterNote(note);

        requestRepository.save(swapRequest);

        // Atualiza o estado do turno para indicar que há uma troca em curso
        turn.setTurnStatus(TurnStatus.SWAP_REQUESTED); //
        turnRepository.save(turn);
    }
}
