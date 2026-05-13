package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.services.TurnService;

import java.util.UUID;

@RestController
@RequestMapping("/api/turns")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<String> acceptTurn(@PathVariable UUID id) {

        boolean sucesso = turnService.acceptTurn(id);

        if (sucesso) {
            return ResponseEntity.ok("Turno aceite e sincronizado com o calendário!");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}