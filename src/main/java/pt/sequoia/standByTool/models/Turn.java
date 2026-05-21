package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    // Relação ManyToOne correta para permitir reutilização em semanas distintas
    @ManyToOne
    @JoinColumn(name = "payment_card_id")
    private Card paymentCard;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "one_off_payment")
    private String oneOffPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Vinculação de múltiplos serviços associados (Ex: Itaú + MG)
    @ManyToMany
    @JoinTable(
            name = "turno_servicos",
            joinColumns = @JoinColumn(name = "turn_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<ServicoCliente> servicosAlocados;

    // ID do evento gerado automaticamente no Google Calendar
    private String calendarEventId;

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