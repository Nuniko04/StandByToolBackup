package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

        // Se não houver serviços alocados, usa o valor por defeito do tipo de turno
        if (turn.getServicosAlocados() == null || turn.getServicosAlocados().isEmpty()) {
            totalValue = turn.getTurnType().getDefaultValue();
        } else {
            String turnTypeName = turn.getTurnType().getName();

            // Soma o valor de cada cliente alocado ao turno, MAS APENAS SE ESTIVER ATIVO
            for (ServicoCliente servico : turn.getServicosAlocados()) {
                if (servico.isAtivo()) { // <-- VERIFICAÇÃO ADICIONADA AQUI
                    if ("StandBy".equalsIgnoreCase(turnTypeName)) {
                        totalValue = totalValue.add(servico.getValorStandby());
                    } else if ("Backup".equalsIgnoreCase(turnTypeName)) {
                        totalValue = totalValue.add(servico.getValorBackup());
                    }
                }
            }
        }

        // Se houver um pagamento único/extra (oneOffPayment) em formato JSON,
        // a lógica para o extrair e somar entraria aqui.

        turn.setTurnValue(totalValue);
        turnRepository.save(turn);

        return totalValue;
    }

    /**
     * Calcula os ganhos estimados de um colaborador num mês e ano específicos.
     * Útil para o ecrã de "Earnings" no Dashboard do Employee.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyEarnings(UUID userId, int month, int year) {
        // Define o início e o fim do mês usando LocalDate (igual ao Turn.java!)
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // Vai buscar todos os turnos do utilizador que começaram nesse mês
        List<Turn> userTurns = turnRepository.findAll().stream() // NOTA: Idealmente criar uma query no TurnRepository para isto
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(userId))
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

        // 3. Processar cada turno encontrado
        for (Turn turno : turnosParaProcessar) {
            calculateAndUpdateTurnValue(turno.getId());
        }

        // Devolve a quantidade de turnos processados
        return turnosParaProcessar.size();
    }
}