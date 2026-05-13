package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.Cartao;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {
}
