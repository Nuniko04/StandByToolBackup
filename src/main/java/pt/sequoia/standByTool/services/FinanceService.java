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

        // 💡 CORREÇÃO APLICADA AQUI:
        // Usamos a nova query que só traz turnos ACCEPTED e COMPLETED!
        List<Turn> turnosDaSemana = turnRepository.findPayableTurnsByEndTimeBetween(inicioSemanaPassada, fimSemanaPassada);

        // Calcular o valor de cada um e "trancar" na base de dados
        for(Turn t : turnosDaSemana) {
            double valorFinal = calcularValorTurno(t);
            t.setTurnValue(BigDecimal.valueOf(valorFinal));
            turnRepository.save(t);
        }
    }

    public double calcularValorTurno(Turn turn) {
        if (turn == null || turn.getTurnType() == null) {
            return 0.0;
        }

        // Data em que o turno acabou
        LocalDateTime dataFimTurno = turn.getEndTime();

        // Quais clientes estavam ativos neste dia?
        List<ServicoCliente> clientesAtivos = servicoClienteRepository.findAtivosNaData(dataFimTurno.toLocalDate());

        // Somar os valores específicos no preçário
        double valorTotalClientes = 0.0;
        for (ServicoCliente cliente : clientesAtivos) {
            // Vai à tabela de preçário ver quanto este cliente paga por este tipo de turno
            ClienteTurnTypeValor precario = precarioRepository.findByClienteAndTurnType(cliente, turn.getTurnType());

            if (precario != null) {
                valorTotalClientes += precario.getValorContribuicao();
            }
        }

        // O valor final é a soma da faturação cruzada dos clientes
        return valorTotalClientes;
    }
}