package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "cartoes")
@Data
@NoArgsConstructor
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identificacaoCartao; // O código do cartão físico

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade; // A validade do cartão que referiste

    @Column(nullable = false)
    private boolean ativo = true; // Para desativar quando caducar ou for perdido
}