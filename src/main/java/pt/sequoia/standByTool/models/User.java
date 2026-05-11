package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.sequoia.standByTool.models.enums.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_oauth_id", unique = true, nullable = false)
    private String googleOauthId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "is_assigner")
    private boolean isAssigner = false;

    @Column(name = "is_employee")
    private boolean isEmployee = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_payment_method", columnDefinition = "jsonb")
    private String defaultPaymentMethod;

    // Opcional, para ajudar nas queries de bloqueio de férias:
    @OneToMany(mappedBy = "requester")
    private List<Request> pedidos;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // --- Lifecycle Methods ---
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}