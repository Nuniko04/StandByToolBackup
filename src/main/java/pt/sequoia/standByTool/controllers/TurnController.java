package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.services.TurnService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/turns")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    // Endpoint para aceitar turno
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptTurn(@PathVariable UUID id) {
        try {
            turnService.acceptTurn(id);
            return ResponseEntity.ok(Map.of("message", "Turno aceite com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint para pedir troca de turno
    @PostMapping("/{id}/swap")
    public ResponseEntity<?> requestSwap(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason"); // Motivo da troca (opcional)
            turnService.requestSwap(id, reason);
            return ResponseEntity.ok(Map.of("message", "Pedido de troca submetido com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}