package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.sequoia.standByTool.models.enums.PaymentStatus;
import pt.sequoia.standByTool.models.enums.TurnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private LocalDate startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDate endTime;

    @Column(name = "turn_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal turnValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_status", length = 50)
    private TurnStatus turnStatus = TurnStatus.PENDING_ACCEPTANCE;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "one_off_payment")
    private String oneOffPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Adicionar esta ligação no Turn.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    private Cartao cartao; // O cartão físico atribuído a este turno específico

    @Column(name = "cartao_data_entrega")
    private LocalDate dataEntregaCartao; // O dia em que lhe deram o cartão para a mão

    // A ligação Mágica: Uma escala pode ter vários serviços associados (Ex: Itaú + MG)
    @ManyToMany
    @JoinTable(
            name = "turno_servicos",
            joinColumns = @JoinColumn(name = "turn_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<ServicoCliente> servicosAlocados;

    // Novo campo para o Calendário (ID do evento no Google/Outlook, gerado automaticamente)
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