package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.HistoricoCartao;

public interface HistoricoCartaoRepository extends JpaRepository<HistoricoCartao, Long> {
}
