package pt.sequoia.standByTool.repositories;

// Importar as classes necessárias se não estiverem
import pt.sequoia.standByTool.models.enums.TurnStatus;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Turn;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TurnRepository extends JpaRepository<Turn, UUID> {

    // Verifica se o utilizador já tem um turno na data
    @Query("SELECT COUNT(t) > 0 FROM Turn t WHERE t.assignee.id = :userId " +
            "AND t.startTime <= :end AND t.endTime >= :start")
    boolean existsByAssigneeAndDates(@Param("userId") UUID userId,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

    @Query(value = "SELECT EXTRACT(WEEK FROM (:now - MAX(start_time))) FROM turns " +
            "WHERE assignee_id = :userId", nativeQuery = true)
    Integer getWeeksSinceLastTurn(@Param("userId") UUID userId, @Param("now") LocalDate now);

    @Query("SELECT COUNT(t) FROM Turn t WHERE t.assignee.id = :userId " +
            "AND t.startTime >= :yearStart AND t.endTime <= :yearEnd")
    int countTurnsInYear(@Param("userId") UUID userId,
                         @Param("yearStart") LocalDate yearStart,
                         @Param("yearEnd") LocalDate yearEnd);

    @Query("SELECT t FROM Turn t WHERE t.turnStatus IN ('ACCEPTED', 'COMPLETED') " +
            "AND t.paymentStatus = 'UNPAID' " +
            "AND t.startTime >= :start AND t.endTime <= :end")
    List<Turn> findUnpaidTurnsInPeriod(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    // 1. Conta quantos feriados o utilizador já apanhou durante os seus turnos num ano específico
    @Query(value = "SELECT COUNT(f.id) FROM turns t " +
            "JOIN feriados f ON f.data >= CAST(t.start_time AS DATE) AND f.data <= CAST(t.end_time AS DATE) " +
            "WHERE t.assignee_id = :userId AND EXTRACT(YEAR FROM t.start_time) = :year",
            nativeQuery = true)
    int countFeriadosTrabalhados(@Param("userId") UUID userId, @Param("year") int year);

    // 2. Conta quantos Fechos de Mês o utilizador já fez num ano específico
    // (Fecho de mês = A semana acaba num dia 28 ou superior, OU a semana atravessa dois meses diferentes)
    @Query(value = "SELECT COUNT(id) FROM turns " +
            "WHERE assignee_id = :userId AND EXTRACT(YEAR FROM start_time) = :year " +
            "AND (EXTRACT(MONTH FROM start_time) != EXTRACT(MONTH FROM end_time) " +
            "     OR EXTRACT(DAY FROM end_time) >= 28)",
            nativeQuery = true)
    int countFechosMesTrabalhados(@Param("userId") UUID userId, @Param("year") int year);

    // Adicionar dentro da interface TurnRepository:
    List<Turn> findByTurnStatusAndStartTimeBetween(TurnStatus status, LocalDate start, LocalDate end);

    // Verifica se já existe um tipo de turno específico nesta data
    @Query("SELECT COUNT(t) > 0 FROM Turn t WHERE t.turnType.name = :typeName AND t.startTime = :start")
    boolean existsTurnOfTypeInWeek(@Param("typeName") String typeName, @Param("start") LocalDate start);

}

