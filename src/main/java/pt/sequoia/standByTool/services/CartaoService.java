package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Cartao;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.CartaoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final AuditLogService auditLogService;

    public CartaoService(CartaoRepository cartaoRepository, AuditLogService auditLogService) {
        this.cartaoRepository = cartaoRepository;
        this.auditLogService = auditLogService;
    }

    public List<Cartao> getAllCartoes() { return cartaoRepository.findAll(); }
    public List<Cartao> getCartoesAtivos() { return cartaoRepository.findByAtivoTrue(); }

    @Transactional
    public Cartao createCartao(String identificacao, LocalDate dataValidade, User adminActor) {
        if (cartaoRepository.existsByIdentificacaoCartao(identificacao)) {
            throw new IllegalArgumentException("Já existe um cartão registado com essa identificação.");
        }

        Cartao cartao = new Cartao();
        cartao.setIdentificacaoCartao(identificacao);
        cartao.setDataValidade(dataValidade);
        cartao.setAtivo(true);
        Cartao saved = cartaoRepository.save(cartao);

        auditLogService.log(adminActor, "CREATE_CARTAO", "Cartao", saved.getId(), "Card registered: " + identificacao);
        return saved;
    }

    @Transactional
    public boolean toggleAtivo(Long id, User adminActor) {
        Optional<Cartao> opt = cartaoRepository.findById(id);
        if (opt.isPresent()) {
            Cartao cartao = opt.get();
            cartao.setAtivo(!cartao.isAtivo());
            cartaoRepository.save(cartao);

            auditLogService.log(adminActor, "TOGGLE_CARTAO_STATUS", "Cartao", cartao.getId(), "Status changed to Active=" + cartao.isAtivo());
            return true;
        }
        return false;
    }
}