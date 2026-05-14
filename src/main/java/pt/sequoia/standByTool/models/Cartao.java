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
    private String identificacaoCartao;

    @OneToOne
    @JoinColumn(name = "colaborador_atual_id")
    private User colaboradorAtual;

    private LocalDate dataEntrega;

    // --- ADICIONA ISTO MANUALMENTE PARA O IDE PARAR DE RECLAMAR ---

    public User getColaboradorAtual() {
        return colaboradorAtual;
    }

    public void setColaboradorAtual(User colaboradorAtual) {
        this.colaboradorAtual = colaboradorAtual;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }
}