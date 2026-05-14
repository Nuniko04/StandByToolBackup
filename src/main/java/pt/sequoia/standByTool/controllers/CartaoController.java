package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.Cartao;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.CartaoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cartoes")
public class CartaoController {

    private final CartaoService cartaoService;

    public CartaoController(CartaoService cartaoService) {
        this.cartaoService = cartaoService;
    }

    // Endpoint: Listar todo o inventário (Admin Dashboard)
    @GetMapping
    public ResponseEntity<List<Cartao>> getAll() {
        return ResponseEntity.ok(cartaoService.getAllCartoes());
    }

    // Endpoint: Listar só os cartões disponíveis para atribuir (Dropdown de nova escala)
    @GetMapping("/ativos")
    public ResponseEntity<List<Cartao>> getAtivos() {
        return ResponseEntity.ok(cartaoService.getCartoesAtivos());
    }

    // Endpoint: Criar um cartão novo
    @PostMapping
    public ResponseEntity<?> createCartao(@RequestBody Map<String, String> payload, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return ResponseEntity.status(403).build();

        try {
            String identificacao = payload.get("identificacaoCartao");
            LocalDate dataValidade = LocalDate.parse(payload.get("dataValidade"));
            return ResponseEntity.ok(cartaoService.createCartao(identificacao, dataValidade, adminActor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Dados inválidos. Verifica a data de validade.");
        }
    }

    // Endpoint: Ativar/Desativar um cartão
    @PutMapping("/{id}/toggle")
    public ResponseEntity<String> toggleStatus(@PathVariable Long id, jakarta.servlet.http.HttpSession session) {
        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return ResponseEntity.status(403).build();

        boolean sucesso = cartaoService.toggleAtivo(id, adminActor);
        return sucesso ? ResponseEntity.ok("Estado atualizado.") : ResponseEntity.notFound().build();
    }
}