package pt.sequoia.standByTool.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pt.sequoia.standByTool.models.ClienteTurnTypeValor;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.repositories.ClienteTurnTypeValorRepository;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinanceService {

    private final ServicoClienteRepository servicoClienteRepository;
    private final ClienteTurnTypeValorRepository precarioRepository;
    private final TurnRepository turnRepository;

    public FinanceService(ServicoClienteRepository servicoClienteRepository, ClienteTurnTypeValorRepository precarioRepository, TurnRepository turnRepository) {
        this.servicoClienteRepository = servicoClienteRepository;
        this.precarioRepository = precarioRepository;
        this.turnRepository = turnRepository;
    }

    @Transactional
    public void calcularEAtualizarValoresDaSemanaAnterior() {
        // Assume-se que o CRON corre à Segunda-feira
        LocalDateTime agora = LocalDateTime.now();

        // A semana anterior começou há 7 dias, à meia-noite (00:00:00)
        LocalDateTime inicioSemanaPassada = agora.minusDays(7).withHour(0).withMinute(0).withSecond(0);

        // A semana anterior terminou ontem, no final do dia (23:59:59)
        LocalDateTime fimSemanaPassada = agora.minusDays(1).withHour(23).withMinute(59).withSecond(59);

        // 1. Procurar os turnos que terminaram dentro deste intervalo
        // (Vais precisar de criar esta query no TurnRepository)
        List<Turn> turnosDaSemana = turnRepository.findByEndTimeBetween(inicioSemanaPassada, fimSemanaPassada);

        // 2. Calcular o valor de cada um e "trancar" na base de dados
        for(Turn t : turnosDaSemana) {
            double valorFinal = calcularValorTurno(t); // O método que já fizemos com a tabela de preçário
            t.setTurnValue(BigDecimal.valueOf(valorFinal));
            turnRepository.save(t);
        }
    }

    public double calcularValorTurno(Turn turn) {
        if (turn == null || turn.getTurnType() == null) {
            return 0.0;
        }

        // 1. Data em que o turno acabou (Nota: Teremos de mudar getEndTime() para LocalDate quando migrarmos para LocalDateTime)
        LocalDateTime dataFimTurno = turn.getEndTime();

        // 2. Quais clientes estavam ativos neste dia?
        List<ServicoCliente> clientesAtivos = servicoClienteRepository.findAtivosNaData(dataFimTurno.toLocalDate());

        // 3. Somar os valores específicos no preçário
        double valorTotalClientes = 0.0;
        for (ServicoCliente cliente : clientesAtivos) {
            // Vai à tabela de preçário ver quanto este cliente paga por este tipo de turno
            ClienteTurnTypeValor precario = precarioRepository.findByClienteAndTurnType(cliente, turn.getTurnType());

            if (precario != null) {
                valorTotalClientes += precario.getValorContribuicao();
            }
        }

        // O valor final é o valor base do turno (se existir) + a soma da faturação cruzada dos clientes
        return valorTotalClientes;
    }
}