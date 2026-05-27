package pt.sequoia.standByTool.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pt.sequoia.standByTool.models.ClienteTurnTypeValor;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.repositories.ClienteTurnTypeValorRepository;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FinanceService {

    private final ServicoClienteRepository servicoClienteRepository;
    private final ClienteTurnTypeValorRepository valorRepository;
    private final TurnRepository turnRepository;

    public FinanceService(ServicoClienteRepository servicoClienteRepository, ClienteTurnTypeValorRepository precarioRepository, TurnRepository turnRepository) {
        this.servicoClienteRepository = servicoClienteRepository;
        this.valorRepository = precarioRepository;
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

    public BigDecimal calcularValorTurno(Turn turno) {
        TurnType tipoTurno = turno.getTurnType();
        List<ServicoCliente> clientesDoTurno = turno.getServicosAlocados();

        if (clientesDoTurno == null || clientesDoTurno.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate dataInicio = turno.getStartTime().toLocalDate();
        LocalDate dataFim = turno.getEndTime().toLocalDate();

        // 💡 CONTAGEM RESTRITA A DIAS ÚTEIS (Segunda a Sexta)
        long numeroDeFeriadosUteis = feriadoRepository.findByDataBetween(dataInicio, dataFim).stream()
                .filter(f -> {
                    java.time.DayOfWeek dia = f.getData().getDayOfWeek();
                    return dia != java.time.DayOfWeek.SATURDAY && dia != java.time.DayOfWeek.SUNDAY;
                })
                .count();

        BigDecimal totalDoTurno = BigDecimal.ZERO;

        for (ServicoCliente cliente : clientesDoTurno) {

            Optional<ClienteTurnTypeValor> contribuicaoOpt = valorRepository.findByClienteAndTurnType(cliente, tipoTurno);

            if (contribuicaoOpt.isPresent()) {
                ClienteTurnTypeValor precos = contribuicaoOpt.get();

                BigDecimal valorACobrarDesteCliente = precos.getValorContribuicao();

                // 💡 Multiplicador ativado apenas por numeroDeFeriadosUteis
                if (numeroDeFeriadosUteis > 0 && precos.getValorContribuicaoFeriado() != null && precos.getValorContribuicaoFeriado().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiplicador = new BigDecimal(numeroDeFeriadosUteis);
                    BigDecimal extraFeriados = precos.getValorContribuicaoFeriado().multiply(multiplicador);
                    valorACobrarDesteCliente = valorACobrarDesteCliente.add(extraFeriados);
                }

                totalDoTurno = totalDoTurno.add(valorACobrarDesteCliente);
            }
        }

        return totalDoTurno;
    }
}