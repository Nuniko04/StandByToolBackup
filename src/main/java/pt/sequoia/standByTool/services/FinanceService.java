package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class FinanceService {

    private final TurnRepository turnRepository;

    public FinanceService(TurnRepository turnRepository) {
        this.turnRepository = turnRepository;
    }

    @Transactional
    public void calcularEAtualizarValoresDoMesAnterior() {
        // 1. Descobrir as datas do mês anterior
        LocalDate hoje = LocalDate.now();
        LocalDate primeiroDiaMesAnterior = hoje.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate ultimoDiaMesAnterior = hoje.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        // Começa às 00:00:00 do primeiro dia, acaba às 23:59:59 do último dia
        OffsetDateTime inicio = primeiroDiaMesAnterior.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fim = ultimoDiaMesAnterior.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        // 2. Ir buscar os turnos ACCEPTED que COMEÇARAM no mês anterior (mesmo que acabem no atual)
        List<Turn> turnos = turnRepository.findByTurnStatusAndStartTimeBetween(TurnStatus.ACCEPTED, inicio, fim);

        // 3. Iterar e calcular o valor de cada turno
        for (Turn turn : turnos) {

            // Como ignoramos a lógica dos clientes por agora, vamos buscar
            // o valor configurado por defeito no TurnType (ex: StandBy = 50€)
            BigDecimal valorBase = turn.getTurnType().getDefaultValue();

            // Atualiza o valor do turno
            turn.setTurnValue(valorBase);
        }

        // 4. Guardar as alterações todas na Base de Dados
        if (!turnos.isEmpty()) {
            turnRepository.saveAll(turnos);
            System.out.println("Motor Financeiro: " + turnos.size() + " turnos processados para o mês anterior.");
        } else {
            System.out.println("Motor Financeiro: Nenhum turno encontrado para processar no mês anterior.");
        }
    }
}