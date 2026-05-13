package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Cartao;
import pt.sequoia.standByTool.models.HistoricoCartao;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.CartaoRepository;
import pt.sequoia.standByTool.repositories.HistoricoCartaoRepository;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final HistoricoCartaoRepository historicoCartaoRepository;
    private final UserRepository userRepository;

    public CartaoService(CartaoRepository cartaoRepository,
                         HistoricoCartaoRepository historicoCartaoRepository,
                         UserRepository userRepository) {
        this.cartaoRepository = cartaoRepository;
        this.historicoCartaoRepository = historicoCartaoRepository;
        this.userRepository = userRepository;
    }

    /**
     * Associa um cartão a um colaborador e cria o registo no histórico.
     */
    @Transactional
    public void atribuirCartao(Long cartaoId, UUID userId) {
        Cartao cartao = cartaoRepository.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));

        if (cartao.getColaboradorAtual() != null) {
            throw new IllegalStateException("Este cartão já está atribuído a outro colaborador.");
        }

        User colaborador = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado."));

        LocalDate dataHoje = LocalDate.now();

        // 1. Atualizar o Cartão
        cartao.setColaboradorAtual(colaborador);
        cartao.setDataEntrega(dataHoje);
        cartaoRepository.save(cartao);

        // 2. Criar o registo no Histórico
        HistoricoCartao historico = new HistoricoCartao();
        historico.setCartao(cartao);
        historico.setColaborador(colaborador);
        historico.setDataEntrega(dataHoje);
        historicoCartaoRepository.save(historico);
    }

    /**
     * Desassocia o cartão do colaborador e preenche a data de devolução no histórico.
     */
    @Transactional
    public void devolverCartao(Long cartaoId) {
        Cartao cartao = cartaoRepository.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));

        if (cartao.getColaboradorAtual() == null) {
            throw new IllegalStateException("Este cartão já se encontra livre na gaveta.");
        }

        // 1. Fechar o registo no Histórico
        HistoricoCartao registoAtivo = historicoCartaoRepository.findRegistoAtivoPorCartao(cartaoId)
                .orElseThrow(() -> new IllegalStateException("Não foi encontrado um registo de histórico ativo para este cartão."));

        registoAtivo.setDataDevolucao(LocalDate.now());
        historicoCartaoRepository.save(registoAtivo);

        // 2. Libertar o Cartão
        cartao.setColaboradorAtual(null);
        cartao.setDataEntrega(null);
        cartaoRepository.save(cartao);
    }
}
