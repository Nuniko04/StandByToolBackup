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
    private String identificacaoCartao; // O número ou código do cartão físico

    // Se estiver null, o cartão está livre na gaveta
    @OneToOne
    @JoinColumn(name = "colaborador_atual_id")
    private User colaboradorAtual;

    private LocalDate dataEntrega;
}