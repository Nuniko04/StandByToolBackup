package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.ServicoClienteService;
import pt.sequoia.standByTool.services.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
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
    public String create(@RequestParam String nomeCliente, @RequestParam String tipoServico, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        servicoClienteService.createServico(
                nomeCliente, tipoServico, adminActor);

        return "redirect:/dashboard?tab=Assigner";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam String nomeCliente, @RequestParam String tipoServico, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        boolean sucesso = servicoClienteService.updateServico(
                id, nomeCliente, tipoServico, adminActor);
        return "redirect:/dashboard?tab=Assigner";
    }

    @PutMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        boolean sucesso = servicoClienteService.toggleAtivo(id, adminActor);
        return "redirect:/dashboard?tab=Assigner";
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