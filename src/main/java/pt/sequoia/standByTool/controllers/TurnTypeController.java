package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnTypeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/turntypes")
public class TurnTypeController {

    private final TurnTypeService turnTypeService;

    public TurnTypeController(TurnTypeService turnTypeService) {
        this.turnTypeService = turnTypeService;
    }

    // Endpoint: GET /api/turntypes
    // O que faz: O frontend chama isto para mostrar os turnos disponíveis quando o Assigner clica em "Assign New Shift"
    @GetMapping
    public ResponseEntity<List<TurnType>> getAll() {
        return ResponseEntity.ok(turnTypeService.getAllTurnTypes());
    }

    // Endpoint: PUT /api/turntypes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody TurnType turnTypeDetails, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return ResponseEntity.status(403).build();

        boolean sucesso = turnTypeService.updateTurnType(
                id, turnTypeDetails.getName(), turnTypeDetails.getDefaultValue(),
                turnTypeDetails.getGoogleCalendarId(), adminActor);

        return sucesso ? ResponseEntity.ok("Tipo de turno atualizado!") : ResponseEntity.notFound().build();
    }
}