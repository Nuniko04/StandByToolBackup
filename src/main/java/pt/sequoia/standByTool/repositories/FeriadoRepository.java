package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Feriado;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FeriadoRepository extends JpaRepository<Feriado, UUID> { // Corrigido de Long para UUID

    boolean existsByData(LocalDate data);

    // 💡 Retorna a lista para filtragem em memória
    List<Feriado> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);


}