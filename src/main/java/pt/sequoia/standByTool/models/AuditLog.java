package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "target_entity", nullable = false)
    private String targetEntity;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    // Removemos o @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "TEXT")
    private String changes;

    @Column(updatable = false)
    private OffsetDateTime timestamp;

    // --- Lifecycle Methods ---
    @PrePersist
    protected void onCreate() {
        timestamp = OffsetDateTime.now();
    }

}