package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/turns")
public class TurnController {

    private final TurnRepository turnRepository;

    public TurnController(TurnRepository turnRepository) {
        this.turnRepository = turnRepository;
    }

    // Endpoint para o botão "Accept Shift"
    @PostMapping("/{id}/accept")
    public ResponseEntity<String> acceptTurn(@PathVariable UUID id) {
        Optional<Turn> turnOpt = turnRepository.findById(id);

        if (turnOpt.isPresent()) {
            Turn turn = turnOpt.get();
            turn.setTurnStatus(TurnStatus.ACCEPTED); // Muda de PENDING_ACCEPTANCE para ACCEPTED
            turnRepository.save(turn);

            // TODO: Aqui vamos adicionar a Sincronização do Google Calendar no futuro!

            return ResponseEntity.ok("Turno aceite com sucesso!");
        }
        return ResponseEntity.notFound().build();
    }
}