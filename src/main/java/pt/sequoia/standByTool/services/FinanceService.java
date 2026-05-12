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

        // 1. Descobrir a janela de tempo (Semana passada: de Segunda a Domingo)
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        // Como isto corre na Segunda-feira de madrugada, recuamos 7 dias para apanhar a Segunda anterior
        OffsetDateTime inicioSemanaAnterior = agora.minusDays(7).truncatedTo(ChronoUnit.DAYS);
        // O fim é o Domingo anterior (ontem) às 23:59:59
        OffsetDateTime fimSemanaAnterior = inicioSemanaAnterior.plusDays(6).withHour(23).withMinute(59).withSecond(59);

        System.out.println("A calcular fecho semanal de: " + inicioSemanaAnterior + " até " + fimSemanaAnterior);

        // 2. Ir buscar os turnos da semana passada que estão ACCEPTED (ou COMPLETED) e UNPAID
        // (Nota: Os internos precisam de criar esta Query no TurnRepository)
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