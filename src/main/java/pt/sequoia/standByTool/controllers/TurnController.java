package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/turns")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @GetMapping
    public ResponseEntity<List<Turn>> getAllTurns() {
        return ResponseEntity.ok(turnService.getAllTurns());
    }

    @GetMapping("/my-turns")
    public ResponseEntity<List<Turn>> getMyTurns(HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(turnService.getMyTurns(loggedUser.getId()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<String> acceptTurn(@PathVariable UUID id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return ResponseEntity.status(401).build();

        boolean sucesso = turnService.acceptTurn(id, loggedUser);
        if (sucesso) {
            return ResponseEntity.ok("Turno aceite e sincronizado!");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignManualTurn(@RequestBody Map<String, String> payload, HttpSession session) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return ResponseEntity.status(403).build();

        try {
            UUID assigneeId = UUID.fromString(payload.get("assigneeId"));
            UUID turnTypeId = UUID.fromString(payload.get("turnTypeId"));
            LocalDate start = LocalDate.parse(payload.get("startDate"));
            LocalDate end = LocalDate.parse(payload.get("endDate"));

            turnService.createManualTurn(assigneeId, turnTypeId, start, end, assigner);
            return ResponseEntity.ok("Turno manual criado!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> editTurn(@PathVariable UUID id, @RequestBody Map<String, String> payload, HttpSession session) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return ResponseEntity.status(403).build();

        LocalDate start = payload.containsKey("startDate") ? LocalDate.parse(payload.get("startDate")) : null;
        LocalDate end = payload.containsKey("endDate") ? LocalDate.parse(payload.get("endDate")) : null;
        Long cartaoId = payload.containsKey("cartaoId") ? Long.parseLong(payload.get("cartaoId")) : null;

        boolean sucesso = turnService.updateTurn(id, start, end, cartaoId, assigner);
        return sucesso ? ResponseEntity.ok("Atualizado!") : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTurn(@PathVariable UUID id, HttpSession session) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return ResponseEntity.status(403).build();

        boolean sucesso = turnService.deleteTurn(id, assigner);
        return sucesso ? ResponseEntity.ok("Apagado!") : ResponseEntity.notFound().build();
    }
}