package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "servicos_cliente")
@EntityListeners(AuditingEntityListener.class) // 💡 OBRIGATÓRIO PARA A AUDITORIA
@Data
@NoArgsConstructor
public class ServicoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeCliente; // Ex: Itaú, Finantia, MG

    @Column(nullable = false)
    private String tipoServico; // Ex: Passive Holiday, Standby 24/7, COB

    @Column(nullable = false)
    private boolean ativo = true; // Para desativar quando perdem o cliente

    @Column(name = "data_fim")
    private LocalDate dataFim;

    // ==========================================
    // 💡 CAMPOS DE AUDITORIA AUTOMÁTICA
    // ==========================================

    @CreatedBy
    @Column(updatable = false)
    private String criadoPor; // Email do Assigner que criou o cliente

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime dataCriacao; // Data e hora em que foi guardado

    @LastModifiedBy
    private String modificadoPor; // Email do último Assigner a editar o cliente

    @LastModifiedDate
    private LocalDateTime dataModificacao; // Data e hora da última edição
}