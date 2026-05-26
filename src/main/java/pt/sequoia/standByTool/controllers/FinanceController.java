package pt.sequoia.standByTool.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.sequoia.standByTool.services.FinanceService;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final String CRON_SECRET = "MinhaChaveSecretaSuperSegura123";

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    // 💡 Rota corrigida para semanal
    @PostMapping("/process-weekly-billing")
    public ResponseEntity<String> processWeeklyBilling(@RequestHeader(value = "X-Cloud-Scheduler-Auth", required = false) String authHeader) {

        if (authHeader == null || !authHeader.equals(CRON_SECRET)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied.");
        }

        try {
            System.out.println("🚀 A iniciar o fecho financeiro da semana anterior...");

            // 💡 Chama o método com o teu nome original/lógica semanal
            financeService.calcularEAtualizarValoresDaSemanaAnterior();

            System.out.println("✅ Fecho financeiro semanal concluído com sucesso.");
            return ResponseEntity.ok("Fecho financeiro da semana concluído!");

        } catch (Exception e) {
            System.err.println("❌ Erro ao processar fecho semanal: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Erro interno: " + e.getMessage());
        }
    }
}