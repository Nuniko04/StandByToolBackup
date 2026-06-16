package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.AuditLogService;
import pt.sequoia.standByTool.services.ServicoClienteService;
import pt.sequoia.standByTool.services.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos-cliente")
public class ServicoClienteController {

    private final ServicoClienteService servicoClienteService;
    private final UserService userService;

    public ServicoClienteController(ServicoClienteService servicoClienteService, UserService userService) {
        this.servicoClienteService = servicoClienteService;
        this.userService = userService;
    }

    // Listar todos (Para o ecrã de Settings)
    @GetMapping
    public ResponseEntity<List<ServicoCliente>> getAll() {
        return ResponseEntity.ok(servicoClienteService.getAllServicos());
    }

    // Listar só os ativos (Para os Dropdowns)
    @GetMapping("/ativos")
    public ResponseEntity<List<ServicoCliente>> getAtivos() {
        return ResponseEntity.ok(servicoClienteService.getServicosAtivos());
    }

    @PostMapping
    public ResponseEntity<ServicoCliente> create(@RequestBody ServicoCliente payload, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        return ResponseEntity.ok(servicoClienteService.createServico(
                payload.getNomeCliente(), payload.getTipoServico(), adminActor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody ServicoCliente payload, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        boolean sucesso = servicoClienteService.updateServico(
                id, payload.getNomeCliente(), payload.getTipoServico(), adminActor);
        return sucesso ? ResponseEntity.ok("Service updated!") : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<String> toggleStatus(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        boolean sucesso = servicoClienteService.toggleAtivo(id, adminActor);
        return sucesso ? ResponseEntity.ok("Status updated.") : ResponseEntity.notFound().build();
    }

    @PostMapping("/{clienteId}/precos/save")
    public String salvarPreco(
            @PathVariable UUID clienteId,
            @RequestParam UUID turnTypeId,
            @RequestParam BigDecimal valorContribuicao,
            @RequestParam(required = false) BigDecimal valorContribuicaoFeriado,
            @AuthenticationPrincipal OidcUser principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);
        // O Controller delega o trabalho pesado para o Service
        servicoClienteService.salvarPreco(clienteId, turnTypeId, valorContribuicao, valorContribuicaoFeriado, adminActor);

        return "redirect:/dashboard?tab=Assigner";
    }

    @PostMapping("/precos/{precoId}/delete")
    public String apagarPreco(@PathVariable UUID precoId,
                              @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        servicoClienteService.apagarPreco(precoId, adminActor);

        return "redirect:/dashboard?tab=Assigner";
    }
}