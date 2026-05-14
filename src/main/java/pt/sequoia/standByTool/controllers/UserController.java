package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController // Nota que mudei de @Controller para @RestController (para devolver JSON)
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Obter os dados de quem está logado (Para mostrar o nome no menu lateral!)
    @GetMapping("/me")
    public ResponseEntity<User> getMe(HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser != null) {
            return ResponseEntity.ok(loggedUser);
        }
        return ResponseEntity.status(401).build();
    }

    // 2. Listar todos os utilizadores (Para o ecrã "Employee Directory")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 3. Ativar/Desativar um utilizador
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<String> toggleStatus(@PathVariable UUID id, HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");

        // Bloqueia se não estiver logado ou não for Assigner
        if (adminActor == null || !adminActor.isAssigner()) return ResponseEntity.status(403).build();

        // Passamos o adminActor para o Serviço poder gravar no AuditLog!
        boolean sucesso = userService.toggleUserStatus(id, adminActor);
        if (sucesso) {
            return ResponseEntity.ok("Estado do utilizador atualizado.");
        }
        return ResponseEntity.notFound().build();
    }

    // 4. Atualizar as permissões (Assigner e Finastra)
    @PutMapping("/{id}/permissions")
    public ResponseEntity<String> updatePermissions(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload, HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");

        // Bloqueia se não estiver logado ou não for Assigner
        if (adminActor == null || !adminActor.isAssigner()) return ResponseEntity.status(403).build();

        boolean isAssigner = payload.getOrDefault("isAssigner", false);
        boolean isFinastraEligible = payload.getOrDefault("isFinastraEligible", false);

        // Passamos o adminActor no final!
        boolean sucesso = userService.updateUserPermissions(id, isAssigner, isFinastraEligible, adminActor);
        if (sucesso) {
            return ResponseEntity.ok("Permissões atualizadas com sucesso!");
        }
        return ResponseEntity.notFound().build();
    }
}