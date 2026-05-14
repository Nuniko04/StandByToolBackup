package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FinanceService {

    private final TurnRepository turnRepository;
    private final ServicoClienteRepository servicoClienteRepository;
    private final AuditLogService auditLogService;

    public FinanceService(TurnRepository turnRepository, ServicoClienteRepository servicoClienteRepository, AuditLogService auditLogService){
        this.servicoClienteRepository = servicoClienteRepository;
        this.turnRepository = turnRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void calcularEAtualizarValoresDaSemanaAnterior() {

        LocalDate inicioSemanaAnterior = LocalDate.now().minusDays(7);
        LocalDate fimSemanaAnterior = inicioSemanaAnterior.plusDays(6);

        System.out.println("A calcular fecho semanal de: " + inicioSemanaAnterior + " até " + fimSemanaAnterior);

        List<Turn> turnosPorPagar = turnRepository.findUnpaidTurnsInPeriod(inicioSemanaAnterior, fimSemanaAnterior);
        List<ServicoCliente> clientesAtivos = servicoClienteRepository.findByAtivoTrue();

        int turnosAtualizados = 0;

        for (Turn turno : turnosPorPagar) {
            BigDecimal valorTotal = turno.getTurnType().getDefaultValue();

            for (ServicoCliente cliente : clientesAtivos) {
                if (turno.getTurnType().getName().equalsIgnoreCase("StandBy")) {
                    valorTotal = valorTotal.add(cliente.getValorStandby());
                } else if (turno.getTurnType().getName().equalsIgnoreCase("Backup")) {
                    valorTotal = valorTotal.add(cliente.getValorBackup());
                }
            }

            turno.setTurnValue(valorTotal);
            turnosAtualizados++;
        }

        turnRepository.saveAll(turnosPorPagar);

        // Registo de auditoria (actor é null porque é uma ação do sistema)
        auditLogService.log(null, "PROCESS_WEEKLY_BILLING", "System", null,
                "Processed billing for period " + inicioSemanaAnterior + " to " + fimSemanaAnterior + ". Turns updated: " + turnosAtualizados);
    }
}