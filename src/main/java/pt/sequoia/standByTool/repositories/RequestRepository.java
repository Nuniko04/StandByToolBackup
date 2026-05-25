package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Request;
import pt.sequoia.standByTool.models.enums.RequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    @Query("SELECT COUNT(r) > 0 FROM Request r WHERE r.requester.id = :userId " +
            "AND r.status = 'ACCEPTED' AND r.requestType = 'VACATION' " +
            "AND r.timeOffStart <= :end AND r.timeOffEnd >= :start")
    boolean hasApprovedVacation(@Param("userId") UUID userId,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    // 1. Verifica se há férias a cruzar com as datas da semana atual
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r WHERE r.requester.id = :userId AND r.requestType = 'VACATION' AND r.status = 'APPROVED' AND r.timeOffStart <= :end AND r.timeOffEnd >= :start")
    boolean hasApprovedVacationOverlapping(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // 2. Verifica se as férias começam num dia MUITO específico (A nossa regra da 2ª feira)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r WHERE r.requester.id = :userId AND r.requestType = 'VACATION' AND r.status = 'APPROVED' AND r.timeOffStart = :targetDate")
    boolean hasApprovedVacationStartingOn(@Param("userId") UUID userId, @Param("targetDate") LocalDate targetDate);

    // Devolve uma Lista normal, filtrando o PENDING, e ordenando da mais recente para a mais antiga
    List<Request> findByStatusNotOrderByCreatedAtDesc(RequestStatus status);
}