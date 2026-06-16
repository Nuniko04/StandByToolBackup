package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "turn_types")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TurnType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;

    @Column(name = "default_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultValue = BigDecimal.ZERO;

    @Column(name = "default_start_time")
    private LocalTime defaultStartTime;

    @Column(name = "default_end_time")
    private LocalTime defaultEndTime;

    @Column(name = "eligible_for_auto_generation", nullable = false)
    private boolean eligibleForAutoGeneration = true;

    @Column(name = "deleted", columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column(nullable = false)
    private String color = "#3498db";

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