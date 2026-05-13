
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

    /**
     * Endpoint para aceitar um turno: POST /api/turns/{id}/accept
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptTurn(@PathVariable UUID id) {
        try {
            turnService.acceptTurn(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint para pedir troca: POST /api/turns/{id}/swap
     */
    @PostMapping("/{id}/swap")
    public ResponseEntity<Void> requestSwap(
            @PathVariable UUID id,
            @RequestParam(required = false) String note) {
        try {
            turnService.requestSwap(id, note);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}