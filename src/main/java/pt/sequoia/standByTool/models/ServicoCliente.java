package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "servicos_cliente")
@Data
@NoArgsConstructor
public class ServicoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeCliente; // Ex: Itaú, Finantia, MG

    @Column(nullable = false)
    private String tipoServico; // Ex: Passive Holiday, Standby 24/7, COB

    @Column(nullable = false)
    private BigDecimal valorStandby; // Ex: 125.00

    @Column(nullable = false)
    private BigDecimal valorBackup; // Ex: 25.00

    @Column(nullable = false)
    private boolean ativo = true; // Para desativar quando perdem o cliente
}