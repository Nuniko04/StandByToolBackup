package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.sequoia.standByTool.services.FinanceService;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    /**
     * Endpoint chamado pelo Google Cloud Scheduler no dia 1 de cada mês.
     * Não recebe parâmetros porque o serviço vai processar sempre o mês anterior.
     */
    @PostMapping("/process-monthly-billing")
    public ResponseEntity<String> processMonthlyBilling() {

        try {
            System.out.println("🚀 A iniciar o processamento financeiro mensal...");

            financeService.calcularEAtualizarValoresDaSemanaAnterior();

            System.out.println("✅ Processamento financeiro concluído com sucesso.");
            return ResponseEntity.ok("Processamento financeiro concluído com sucesso!");

        } catch (Exception e) {
            System.err.println("❌ Erro ao processar billing: " + e.getMessage());
            // Devolve erro 500 para o Google Cloud Scheduler saber que falhou e tentar novamente
            return ResponseEntity.internalServerError().body("Erro interno ao processar pagamentos: " + e.getMessage());
        }
    }
}