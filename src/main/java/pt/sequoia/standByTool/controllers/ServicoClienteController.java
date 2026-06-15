package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.ServicoClienteService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicos-cliente")
public class ServicoClienteController {

    private final ServicoClienteService servicoClienteService;

    public ServicoClienteController(ServicoClienteService servicoClienteService) {
        this.servicoClienteService = servicoClienteService;
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
    public ResponseEntity<ServicoCliente> create(@RequestBody ServicoCliente payload, HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        return ResponseEntity.ok(servicoClienteService.createServico(
                payload.getNomeCliente(), payload.getTipoServico(), adminActor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody ServicoCliente payload, HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        boolean sucesso = servicoClienteService.updateServico(
                id, payload.getNomeCliente(), payload.getTipoServico(), adminActor);
        return sucesso ? ResponseEntity.ok("Service updated!") : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<String> toggleStatus(@PathVariable UUID id, HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        boolean sucesso = servicoClienteService.toggleAtivo(id, adminActor);
        return sucesso ? ResponseEntity.ok("Status updated.") : ResponseEntity.notFound().build();
    }

    @PostMapping("/{clienteId}/precos/save")
    public String salvarPreco(
            @PathVariable UUID clienteId,
            @RequestParam UUID turnTypeId,
            @RequestParam BigDecimal valorContribuicao,
            @RequestParam(required = false) BigDecimal valorContribuicaoFeriado,
            HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        // O Controller delega o trabalho pesado para o Service
        servicoClienteService.salvarPreco(clienteId, turnTypeId, valorContribuicao, valorContribuicaoFeriado, adminActor);

        return "redirect:/dashboard?tab=Assigner";
    }

    @PostMapping("/precos/{precoId}/delete")
    public String apagarPreco(@PathVariable UUID precoId,
                              HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        servicoClienteService.apagarPreco(precoId, adminActor);

        return "redirect:/dashboard?tab=Assigner";
    }
}