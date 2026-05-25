package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TurnRepository extends JpaRepository<Turn, UUID> {

    // 💡 Devolve todos os turnos EXCETO os que têm um determinado status (CANCELLED)
    List<Turn> findByTurnStatusNot(TurnStatus status);

    // 💡 Devolve os turnos de uma pessoa, ordenados, EXCETO os cancelados
    List<Turn> findByAssigneeIdAndTurnStatusNotOrderByStartTimeAscCreatedAtAsc(UUID assigneeId, TurnStatus status);

    // Verifica se o utilizador já tem um turno na data (Protegido contra CANCELLED)
    @Query("SELECT COUNT(t) > 0 FROM Turn t WHERE t.assignee.id = :userId " +
            "AND t.turnStatus != 'CANCELLED' " +
            "AND t.startTime <= :end AND t.endTime >= :start")
    boolean existsByAssigneeAndDates(@Param("userId") UUID userId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    // Protegido contra CANCELLED
    @Query(value = "SELECT EXTRACT(WEEK FROM (:now - MAX(start_time))) FROM turns " +
            "WHERE assignee_id = :userId AND turn_status != 'CANCELLED'", nativeQuery = true)
    Integer getWeeksSinceLastTurn(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    // Protegido contra CANCELLED
    @Query("SELECT COUNT(t) FROM Turn t WHERE t.assignee.id = :userId " +
            "AND t.turnStatus != 'CANCELLED' " +
            "AND t.startTime >= :yearStart AND t.endTime <= :yearEnd")
    int countTurnsInYear(@Param("userId") UUID userId,
                         @Param("yearStart") LocalDateTime yearStart,
                         @Param("yearEnd") LocalDateTime yearEnd);

    // Já está protegido porque só procura ACCEPTED ou COMPLETED
    @Query("SELECT t FROM Turn t WHERE t.turnStatus IN ('ACCEPTED', 'COMPLETED') " +
            "AND t.paymentStatus = 'UNPAID' " +
            "AND t.startTime >= :start AND t.endTime <= :end")
    List<Turn> findUnpaidTurnsInPeriod(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // 1. Conta quantos feriados o utilizador apanhou (Protegido contra CANCELLED)
    @Query(value = "SELECT COUNT(f.id) FROM turns t " +
            "JOIN feriados f ON f.data >= CAST(t.start_time AS DATE) AND f.data <= CAST(t.end_time AS DATE) " +
            "WHERE t.assignee_id = :userId AND EXTRACT(YEAR FROM t.start_time) = :year " +
            "AND t.turn_status != 'CANCELLED'",
            nativeQuery = true)
    int countFeriadosTrabalhados(@Param("userId") UUID userId, @Param("year") int year);

    // 2. Conta quantos Fechos de Mês o utilizador já fez (Protegido contra CANCELLED)
    @Query(value = "SELECT COUNT(id) FROM turns " +
            "WHERE assignee_id = :userId AND EXTRACT(YEAR FROM start_time) = :year " +
            "AND turn_status != 'CANCELLED' " +
            "AND (EXTRACT(MONTH FROM start_time) != EXTRACT(MONTH FROM end_time) " +
            "     OR EXTRACT(DAY FROM end_time) >= 28)",
            nativeQuery = true)
    int countFechosMesTrabalhados(@Param("userId") UUID userId, @Param("year") int year);

    // Verifica se já existe um tipo de turno específico nesta data (Protegido contra CANCELLED)
    @Query("SELECT COUNT(t) > 0 FROM Turn t WHERE t.turnType.name = :typeName AND t.startTime = :start " +
            "AND t.turnStatus != 'CANCELLED'")
    boolean existsTurnOfTypeInWeek(@Param("typeName") String typeName, @Param("start") LocalDateTime start);

    // Protegido contra CANCELLED
    List<Turn> findByEndTimeBetweenAndTurnStatusNot(LocalDateTime inicioSemanaPassada, LocalDateTime fimSemanaPassada, TurnStatus status);

    // --- NOVA TRANCA TEMPORAL DE CARTÕES ---
    @Query("SELECT COUNT(t) FROM Turn t WHERE t.paymentCard.id = :cardId " +
            "AND t.id <> :turnId " +
            "AND t.turnStatus != 'CANCELLED' " +
            "AND (t.startTime < :newEndTime AND t.endTime > :newStartTime)")
    int countOverlappingTurnsWithCard(@Param("cardId") UUID cardId,
                                      @Param("turnId") UUID turnId,
                                      @Param("newStartTime") LocalDateTime newStartTime,
                                      @Param("newEndTime") LocalDateTime newEndTime);

    // 🛡️ QUERY FINANCEIRA: Só vai buscar turnos que efetivamente aconteceram (ACCEPTED ou COMPLETED)
    @Query("SELECT t FROM Turn t WHERE t.turnStatus IN ('ACCEPTED', 'COMPLETED') " +
            "AND t.endTime BETWEEN :start AND :end")
    List<Turn> findPayableTurnsByEndTimeBetween(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}