package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "cliente_turntype_valores")
@EntityListeners(AuditingEntityListener.class)
public class ClienteTurnTypeValor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ServicoCliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_type_id", nullable = false)
    private TurnType turnType;

    @Column(name = "valor_contribuicao", nullable = false)
    private BigDecimal valorContribuicao; // 💡 Rigor nos cêntimos

    // 💡 NOVO CAMPO: Valor específico se o turno apanhar um Feriado
    @Column(name = "valor_contribuicao_feriado")
    private BigDecimal valorContribuicaoFeriado;

    // --- CAMPOS DE AUDITORIA AUTOMÁTICA ---
    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private String criadoPor;

    @CreatedDate
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedBy
    @Column(name = "modificado_por")
    private String modificadoPor;

    @LastModifiedDate
    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;
}