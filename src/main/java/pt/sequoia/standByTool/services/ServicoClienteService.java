package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.ServicoClienteRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ServicoClienteService {

    private final ServicoClienteRepository servicoClienteRepository;
    private final AuditLogService auditLogService;

    public ServicoClienteService(ServicoClienteRepository servicoClienteRepository, AuditLogService auditLogService) {
        this.servicoClienteRepository = servicoClienteRepository;
        this.auditLogService = auditLogService;
    }

    public List<ServicoCliente> getAllServicos() { return servicoClienteRepository.findAll(); }
    public List<ServicoCliente> getServicosAtivos() { return servicoClienteRepository.findByAtivoTrue(); }

    @Transactional
    public ServicoCliente createServico(String nome, String tipo, BigDecimal valorStandby, BigDecimal valorBackup, User adminActor) {
        ServicoCliente servico = new ServicoCliente();
        servico.setNomeCliente(nome);
        servico.setTipoServico(tipo);
        servico.setValorStandby(valorStandby);
        servico.setValorBackup(valorBackup);
        servico.setAtivo(true);
        ServicoCliente saved = servicoClienteRepository.save(servico);

        auditLogService.log(adminActor, "CREATE_SERVICO_CLIENTE", "ServicoCliente", saved.getId(), "Created client: " + nome);
        return saved;
    }

    @Transactional
    public boolean updateServico(Long id, String nome, String tipo, BigDecimal valorStandby, BigDecimal valorBackup, User adminActor) {
        Optional<ServicoCliente> opt = servicoClienteRepository.findById(id);
        if (opt.isPresent()) {
            ServicoCliente servico = opt.get();
            if (nome != null && !nome.isBlank()) servico.setNomeCliente(nome);
            if (tipo != null && !tipo.isBlank()) servico.setTipoServico(tipo);
            if (valorStandby != null) servico.setValorStandby(valorStandby);
            if (valorBackup != null) servico.setValorBackup(valorBackup);

            servicoClienteRepository.save(servico);
            auditLogService.log(adminActor, "UPDATE_SERVICO_CLIENTE", "ServicoCliente", servico.getId(), "Values updated for: " + servico.getNomeCliente());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean toggleAtivo(Long id, User adminActor) {
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
}