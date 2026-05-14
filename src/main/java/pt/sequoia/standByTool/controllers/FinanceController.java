package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.services.FinanceService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    // Injeção de dependência do serviço financeiro
    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    /**
     * Endpoint para processar os turnos ACCEPTED do mês anterior.
     * O Google Cloud Scheduler vai chamar este endpoint via POST no dia 1 de cada mês.
     */
    @PostMapping("/trigger-monthly-calc")
    public ResponseEntity<String> processPreviousMonth() {
        try {
            // Repara aqui: estamos a usar o nome correto do método que está no FinanceService
            int turnosProcessados = financeService.processPreviousMonthAcceptedTurns();
            return ResponseEntity.ok("Fecho de mês concluído com sucesso. Turnos atualizados: " + turnosProcessados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar o mês anterior: " + e.getMessage());
        }
    }

    /**
     * Endpoint para recalcular e atualizar o valor de um turno específico.
     * Pode ser acionado remotamente via POST.
     */
    @PostMapping("/calculate/{turnId}")
    public ResponseEntity<BigDecimal> calculateTurnValue(@PathVariable UUID turnId) {
        try {
            BigDecimal valorFinal = financeService.calculateAndUpdateTurnValue(turnId);
            return ResponseEntity.ok(valorFinal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para consultar os ganhos mensais de um colaborador.
     */
    @GetMapping("/earnings/{userId}")
    public ResponseEntity<BigDecimal> getMonthlyEarnings(
            @PathVariable UUID userId,
            @RequestParam int month,
            @RequestParam int year) {

        BigDecimal ganhos = financeService.calculateMonthlyEarnings(userId, month, year);
        return ResponseEntity.ok(ganhos);
    }
}