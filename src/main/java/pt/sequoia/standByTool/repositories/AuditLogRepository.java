package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.AuditLog;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}