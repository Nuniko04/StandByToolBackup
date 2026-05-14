package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private final TurnRepository turnRepository;

    public FinanceService(TurnRepository turnRepository) {
        this.turnRepository = turnRepository;
    }

    /**
     * Calcula e atualiza o valor de um turno com base nos serviços de cliente ATIVOS alocados.
     */
    @Transactional
    public BigDecimal calculateAndUpdateTurnValue(UUID turnId) {
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));

        BigDecimal totalValue = BigDecimal.ZERO;

        if (turn.getServicosAlocados() == null || turn.getServicosAlocados().isEmpty()) {
            totalValue = turn.getTurnType().getDefaultValue();
        } else {
            String turnTypeName = turn.getTurnType().getName();

            for (ServicoCliente servico : turn.getServicosAlocados()) {
                if (servico.isAtivo()) {
                    if ("StandBy".equalsIgnoreCase(turnTypeName)) {
                        totalValue = totalValue.add(servico.getValorStandby());
                    } else if ("Backup".equalsIgnoreCase(turnTypeName)) {
                        totalValue = totalValue.add(servico.getValorBackup());
                    }
                }
            }
        }

        turn.setTurnValue(totalValue);
        turnRepository.save(turn);

        return totalValue;
    }

    /**
     * Calcula os ganhos estimados de um colaborador num mês e ano específicos.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyEarnings(UUID userId, int month, int year) {

        // Mantemos tudo como LocalDate
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        List<Turn> userTurns = turnRepository.findAll().stream()
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(userId))
                // Comparamos LocalDate com LocalDate (usamos !isBefore e !isAfter para incluir o 1º e o último dia)
                // Usamos !isBefore e !isAfter para incluir o primeiro e último dia do mês
                .filter(t -> !t.getStartTime().isBefore(startOfMonth) && !t.getStartTime().isAfter(endOfMonth))
                .filter(t -> t.getTurnStatus() == TurnStatus.ACCEPTED || t.getTurnStatus() == TurnStatus.COMPLETED)
                .toList();

        return userTurns.stream()
                .map(Turn::getTurnValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Vai à BD buscar os turnos ACCEPTED do mês anterior e atualiza o seu valor.
     */
    @Transactional
    public int processPreviousMonthAcceptedTurns() {
        // 1. Descobrir as datas do mês passado usando LocalDate
        LocalDate hoje = LocalDate.now();
        LocalDate inicioDoMesPassado = hoje.minusMonths(1).withDayOfMonth(1);
        LocalDate fimDoMesPassado = hoje.withDayOfMonth(1).minusDays(1);

        // 2. Ir à BD buscar todos os turnos ACCEPTED desse período
        List<Turn> turnosParaProcessar = turnRepository.findAcceptedTurnsInPeriod(inicioDoMesPassado, fimDoMesPassado);

        for (Turn turno : turnosParaProcessar) {
            calculateAndUpdateTurnValue(turno.getId());
        }

        return turnosParaProcessar.size();
    }
}