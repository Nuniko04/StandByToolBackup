package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.controllers.FinanceController;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;
// imports...

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FinanceService {

    private final TurnRepository turnRepository;
    private final ServicoClienteRepository servicoClienteRepository;

    // Construtor...
    public FinanceService(TurnRepository turnRepository, ServicoClienteRepository servicoClienteRepository){
        this.servicoClienteRepository = servicoClienteRepository;
        this.turnRepository = turnRepository;
    }

    @Transactional
    public void calcularEAtualizarValoresDaSemanaAnterior() {

        // Como isto corre na Segunda-feira, recuamos 7 dias
        LocalDate inicioSemanaAnterior = LocalDate.now().minusDays(7);
        // O fim é o Domingo anterior (ontem)
        LocalDate fimSemanaAnterior = inicioSemanaAnterior.plusDays(6);

        System.out.println("A calcular fecho semanal de: " + inicioSemanaAnterior + " até " + fimSemanaAnterior);

        List<Turn> turnosPorPagar = turnRepository.findUnpaidTurnsInPeriod(inicioSemanaAnterior, fimSemanaAnterior);
        // 3. Ir buscar os Clientes Ativos HOJE (como corre logo a seguir à semana, é super preciso)
        List<ServicoCliente> clientesAtivos = servicoClienteRepository.findByAtivoTrue();


        // 4. A Matemática
        for (Turn turno : turnosPorPagar) {
            BigDecimal valorTotal = turno.getTurnType().getDefaultValue();

            for (ServicoCliente cliente : clientesAtivos) {
                if (turno.getTurnType().getName().equalsIgnoreCase("StandBy")) {
                    valorTotal = valorTotal.add(cliente.getValorStandby());
                } else if (turno.getTurnType().getName().equalsIgnoreCase("Backup")) {
                    valorTotal = valorTotal.add(cliente.getValorBackup());
                }

                // (Opcional) Guardar histórico para faturação futura
                // turno.getServicosAlocados().add(cliente);
            }

            // 5. Atualizar Valores
            turno.setTurnValue(valorTotal);
            // turno.setPaymentStatus(PaymentStatus.PAID); // Depende se querem fechar já o pagamento
        }

        turnRepository.saveAll(turnosPorPagar);

    }
}