package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.HistoricoCartao;

import java.util.Optional;

public interface HistoricoCartaoRepository extends JpaRepository<HistoricoCartao, Long> {

    // Procura o registo de histórico que ainda não tem data de devolução preenchida para um dado cartão
    @Query("SELECT h FROM HistoricoCartao h WHERE h.cartao.id = :cartaoId AND h.dataDevolucao IS NULL")
    Optional<HistoricoCartao> findRegistoAtivoPorCartao(@Param("cartaoId") Long cartaoId);
}
