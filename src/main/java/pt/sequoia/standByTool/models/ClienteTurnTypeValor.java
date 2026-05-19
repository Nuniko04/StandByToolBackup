package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "cliente_turntype_valores")
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
    private double valorContribuicao;
}