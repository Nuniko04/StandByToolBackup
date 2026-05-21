package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.AuditLog;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.AuditLogRepository;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // REQUIRES_NEW garante que o log é gravado mesmo que a transação principal falhe depois
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, String action, String entity, UUID targetId, String changes) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setActionType(action);
        log.setTargetEntity(entity);
        log.setTargetId(targetId);
        log.setChanges(changes);
        auditLogRepository.save(log);
    }
}