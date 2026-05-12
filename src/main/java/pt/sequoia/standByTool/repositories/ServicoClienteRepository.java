package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.ServicoCliente;
import java.util.List;

public interface ServicoClienteRepository extends JpaRepository<ServicoCliente, Long> {

    // O Spring Data JPA cria a query automaticamente por causa do nome do método!
    List<ServicoCliente> findByAtivoTrue();
}