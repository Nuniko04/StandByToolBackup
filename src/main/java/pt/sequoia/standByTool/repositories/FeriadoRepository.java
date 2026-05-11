package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Feriado;
import java.time.LocalDate;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    boolean existsByData(LocalDate data);

    @Query("SELECT COUNT(f) > 0 FROM Feriado f WHERE f.data BETWEEN :start AND :end")
    boolean existsFeriadoInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);
}