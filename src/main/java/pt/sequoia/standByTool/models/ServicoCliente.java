package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "servicos_cliente")
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

    // ADICIONA ISTO:
    @Column(name = "data_fim")
    private LocalDate dataFim;
}