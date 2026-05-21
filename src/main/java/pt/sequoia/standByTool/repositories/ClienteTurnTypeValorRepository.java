package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.ClienteTurnTypeValor;
import pt.sequoia.standByTool.models.ServicoCliente;
import pt.sequoia.standByTool.models.TurnType;

import java.util.UUID;

public interface ClienteTurnTypeValorRepository extends JpaRepository<ClienteTurnTypeValor, UUID> {

    // A query mágica que o Spring cria automaticamente para encontrar o preço exato!
    ClienteTurnTypeValor findByClienteAndTurnType(ServicoCliente cliente, TurnType turnType);
}