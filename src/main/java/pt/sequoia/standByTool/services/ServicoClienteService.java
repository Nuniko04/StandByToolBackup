package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.ClienteTurnTypeValor;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.ClienteTurnTypeValorRepository;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;
import pt.sequoia.standByTool.repositories.TurnTypeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServicoClienteService {

    private final ServicoClienteRepository servicoClienteRepository;
    private final AuditLogService auditLogService;
    private final ClienteTurnTypeValorRepository valorRepository; // 💡 Injetado no Service!
    private final TurnTypeRepository turnTypeRepository;

    public ServicoClienteService(ServicoClienteRepository servicoClienteRepository, AuditLogService auditLogService, ClienteTurnTypeValorRepository valorRepository,
                                 TurnTypeRepository turnTypeRepository) {
        this.servicoClienteRepository = servicoClienteRepository;
        this.auditLogService = auditLogService;
        this.valorRepository = valorRepository;
        this.turnTypeRepository = turnTypeRepository;
    }

    public List<ServicoCliente> getAllServicos() { return servicoClienteRepository.findAll(); }
    public List<ServicoCliente> getServicosAtivos() { return servicoClienteRepository.findByAtivoTrue(); }

    @Transactional
    public ServicoCliente createServico(String nome, String tipo, User adminActor) {
        ServicoCliente servico = new ServicoCliente();
        servico.setNomeCliente(nome);
        servico.setTipoServico(tipo);
        servico.setAtivo(true);
        ServicoCliente saved = servicoClienteRepository.save(servico);

        auditLogService.log(adminActor, "CREATE_SERVICO_CLIENTE", "ServicoCliente", saved.getId(), "Created client: " + nome);
        return saved;
    }

    @Transactional
    public boolean updateServico(UUID id, String nome, String tipo, User adminActor) {
        Optional<ServicoCliente> opt = servicoClienteRepository.findById(id);
        if (opt.isPresent()) {
            ServicoCliente servico = opt.get();
            if (nome != null && !nome.isBlank()) servico.setNomeCliente(nome);
            if (tipo != null && !tipo.isBlank()) servico.setTipoServico(tipo);

            servicoClienteRepository.save(servico);
            auditLogService.log(adminActor, "UPDATE_SERVICO_CLIENTE", "ServicoCliente", servico.getId(), "Values updated for: " + servico.getNomeCliente());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean toggleAtivo(UUID id, User adminActor) {
        Optional<ServicoCliente> opt = servicoClienteRepository.findById(id);
        if (opt.isPresent()) {
            ServicoCliente servico = opt.get();
            servico.setAtivo(!servico.isAtivo());
            servicoClienteRepository.save(servico);

            auditLogService.log(adminActor, "TOGGLE_SERVICO_CLIENTE", "ServicoCliente", servico.getId(), "Status changed to Active=" + servico.isAtivo());
            return true;
        }
        return false;
    }

    // 💡 LÓGICA DE SALVAR/ATUALIZAR (Upsert)
    @Transactional
    public void salvarPreco(UUID clienteId, UUID turnTypeId, BigDecimal valorContribuicao, BigDecimal valorFeriado, User adminActor) {
        ServicoCliente cliente = servicoClienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        TurnType turnType = turnTypeRepository.findById(turnTypeId)
                .orElseThrow(() -> new IllegalArgumentException("TurnType não encontrado"));

        ClienteTurnTypeValor preco = valorRepository.findByClienteAndTurnType(cliente, turnType)
                .orElse(new ClienteTurnTypeValor());

        preco.setCliente(cliente);
        preco.setTurnType(turnType);
        preco.setValorContribuicao(valorContribuicao);
        preco.setValorContribuicaoFeriado(valorFeriado);

        valorRepository.save(preco);

        auditLogService.log(adminActor, "SAVE_CLIENT_PRICE", "ServicoCliente", clienteId, "Price saved/updated for: " + cliente.getNomeCliente());
    }

    // 💡 LÓGICA DE APAGAR
    @Transactional
    public void apagarPreco(UUID precoId, User adminActor) {
        if (valorRepository.existsById(precoId)) {
            ServicoCliente cliente = valorRepository.findById(precoId).get().getCliente();
            valorRepository.deleteById(precoId);
            auditLogService.log(adminActor, "CLEAR_CLIENT_PRICE", "ServicoCliente", precoId, "Price cleared for: " + cliente.getNomeCliente());
        }
    }
}