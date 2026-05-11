package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "historico_cartoes")
@Data
@NoArgsConstructor
public class HistoricoCartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cartao_id", nullable = false)
    private Cartao cartao;

    @ManyToOne
    @JoinColumn(name = "colaborador_id", nullable = false)
    private User colaborador;

    @Column(nullable = false)
    private LocalDate dataEntrega;

    private LocalDate dataDevolucao; // Quando devolve o cartão, esta data é preenchida
}