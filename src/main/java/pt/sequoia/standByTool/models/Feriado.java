package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.sequoia.standByTool.models.enums.TipoFeriado;

import java.time.LocalDate;

@Entity
@Table(name = "feriados")
@Data
@NoArgsConstructor
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data; // Ex: 2026-04-03

    @Column(nullable = false)
    private String nome; // Ex: Sexta Feira Santa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFeriado tipo; // ENUM: PASSIVO, ATIVO

    @Column(nullable = false)
    private boolean billable; // True para dias de semana, False para fins de semana
}