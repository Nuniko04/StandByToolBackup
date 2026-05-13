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

    // O Google Cloud Scheduler vai chamar este endpoint via POST
    @PostMapping("/trigger-monthly-calc")
    public ResponseEntity<String> fecharMesFinanceiro() {
        try {
            financeService.calcularEAtualizarValoresDoMesAnterior();
            return ResponseEntity.ok("Sucesso: Processamento financeiro do mês anterior concluído.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar as finanças: " + e.getMessage());
        }
    }
}