package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.ServicoClienteService;

import java.util.List;

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
    public ResponseEntity<ServicoCliente> create(@RequestBody ServicoCliente payload, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        return ResponseEntity.ok(servicoClienteService.createServico(
                payload.getNomeCliente(), payload.getTipoServico(), adminActor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody ServicoCliente payload, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        boolean sucesso = servicoClienteService.updateServico(
                id, payload.getNomeCliente(), payload.getTipoServico(), adminActor);
        return sucesso ? ResponseEntity.ok("Service updated!") : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<String> toggleStatus(@PathVariable Long id, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        boolean sucesso = servicoClienteService.toggleAtivo(id, adminActor);
        return sucesso ? ResponseEntity.ok("Status updated.") : ResponseEntity.notFound().build();
    }
}