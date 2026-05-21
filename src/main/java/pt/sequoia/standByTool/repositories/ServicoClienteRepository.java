package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.ServicoCliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ServicoClienteRepository extends JpaRepository<ServicoCliente, Long> {

    // O Spring Data JPA cria a query automaticamente por causa do nome do método!
    List<ServicoCliente> findByAtivoTrue();

    // ADICIONA ISTO:
    @Query("SELECT s FROM ServicoCliente s WHERE s.ativo = true AND (s.dataFim IS NULL OR s.dataFim >= :dataAlvo)")
    List<ServicoCliente> findAtivosNaData(@Param("dataAlvo") LocalDate dataAlvo);
}