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

    @Transactional
    public Cartao atribuirCartao(Long cartaoId, UUID userId) {
        Cartao cartao = cartaoRepository.findById(cartaoId)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        User utilizador = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        if (cartao.getColaboradorAtual() != null) {
            throw new RuntimeException("Este cartão já está atribuído a outra pessoa. Tem de ser devolvido primeiro.");
        }

        // Atribui o cartão
        cartao.setColaboradorAtual(utilizador);
        cartao.setDataEntrega(LocalDate.now());

        return cartaoRepository.save(cartao);
    }

}