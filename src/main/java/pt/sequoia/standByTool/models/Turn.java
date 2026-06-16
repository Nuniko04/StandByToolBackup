package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pt.sequoia.standByTool.models.enums.PaymentMethod;
import pt.sequoia.standByTool.models.enums.PaymentStatus;
import pt.sequoia.standByTool.models.enums.TurnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "turns")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Turn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_type_id")
    private TurnType turnType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "turn_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal turnValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_status", length = 50)
    private TurnStatus turnStatus = TurnStatus.PENDING_ACCEPTANCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // --- LOGÍSTICA DE PAGAMENTO ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    // --- LIGAÇÃO COM O CARTÃO ---
    @ManyToOne
    @JoinColumn(name = "payment_card_id")
    private Card paymentCard;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "one_off_payment")
    private String oneOffPayment;

    // MANTIDO: O teu link antigo manual para o utilizador
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToMany
    @JoinTable(
            name = "turno_servicos",
            joinColumns = @JoinColumn(name = "turn_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<ServicoCliente> servicosAlocados;

    // ID do evento gerado automaticamente no Google Calendar
    private String calendarEventId;

    // --- CAMPOS DE AUDITORIA AUTOMÁTICA ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "criado_por_email", updatable = false)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "modificado_por_email")
    private String modificadoPor;
}