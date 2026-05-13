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

    public TurnService(TurnRepository turnRepository, RequestRepository requestRepository) {
        this.turnRepository = turnRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public Turn acceptTurn(UUID turnId) {
        // Vai buscar o turno à BD
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new RuntimeException("Turno não encontrado"));

        // Valida se o turno está pendente
        if (turn.getTurnStatus() != TurnStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Este turno não está pendente de aceitação.");
        }

        // Muda o estado para ACCEPTED
        turn.setTurnStatus(TurnStatus.ACCEPTED);
        return turnRepository.save(turn);
    }

    @Transactional
    public Request requestSwap(UUID turnId, String reason) {
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new RuntimeException("Turno não encontrado"));

        // Muda o estado do turno
        turn.setTurnStatus(TurnStatus.SWAP_REQUESTED);
        turnRepository.save(turn);

        // Cria o pedido de troca (Request)
        Request swapRequest = new Request();
        swapRequest.setRequestType(RequestType.TURN_SWAP);
        swapRequest.setRequester(turn.getAssignee()); // O dono do turno é quem pede a troca
        swapRequest.setTurn(turn);
        swapRequest.setStatus(RequestStatus.PENDING);
        swapRequest.setRequesterNote(reason);

        return requestRepository.save(swapRequest);
    }
}