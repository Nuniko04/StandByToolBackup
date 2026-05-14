package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.Request;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.services.RequestService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // O Assigner precisa de ver todos os pedidos pendentes
    @GetMapping("/pending")
    public ResponseEntity<List<Request>> getPending() {
        return ResponseEntity.ok(requestService.getPendingRequests());
    }

    // O Colaborador precisa de ver o histórico dos seus próprios pedidos
    @GetMapping("/my-requests")
    public ResponseEntity<List<Request>> getMyRequests(HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(requestService.getRequestsByUser(loggedUser.getId()));
    }

    // Colaborador submete Férias
    @PostMapping("/time-off")
    public ResponseEntity<String> requestTimeOff(@RequestBody Map<String, String> payload, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return ResponseEntity.status(401).build();

        LocalDate start = LocalDate.parse(payload.get("startDate"));
        LocalDate end = LocalDate.parse(payload.get("endDate"));
        String note = payload.get("note");

        requestService.createTimeOffRequest(loggedUser.getId(), start, end, note);
        return ResponseEntity.ok("Pedido de ausência submetido com sucesso!");
    }

    // Colaborador submete Troca
    @PostMapping("/swap")
    public ResponseEntity<String> requestSwap(@RequestBody Map<String, String> payload, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return ResponseEntity.status(401).build();

        UUID turnId = UUID.fromString(payload.get("turnId"));
        String note = payload.get("note");

        requestService.createSwapRequest(loggedUser.getId(), turnId, note);
        return ResponseEntity.ok("Pedido de troca submetido com sucesso!");
    }

    // Assigner processa (Aprova/Rejeita)
    @PutMapping("/{id}/process")
    public ResponseEntity<String> processRequest(@PathVariable UUID id, @RequestBody Map<String, String> payload, HttpSession session) {
        User assigner = (User) session.getAttribute("loggedUser");
        // Validação de segurança: Só Assigners podem aprovar/rejeitar
        if (assigner == null || !assigner.isAssigner()) return ResponseEntity.status(403).body("Acesso negado.");

        RequestStatus status = RequestStatus.valueOf(payload.get("status")); // 'APPROVED' ou 'DENIED'
        String note = payload.get("assignerNote");

        // Se for uma troca e for aprovada, o frontend tem de mandar quem é o substituto
        UUID newAssigneeId = payload.containsKey("newAssigneeId") && payload.get("newAssigneeId") != null
                ? UUID.fromString(payload.get("newAssigneeId")) : null;

        try {
            boolean sucesso = requestService.processRequest(id, assigner.getId(), status, note, newAssigneeId);
            if (sucesso) return ResponseEntity.ok("Pedido " + status + " com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.notFound().build();
    }
}