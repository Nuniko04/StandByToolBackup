package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.sequoia.standByTool.models.Request;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    @Query("SELECT COUNT(r) > 0 FROM Request r WHERE r.requester.id = :userId " +
            "AND r.status = 'ACCEPTED' AND r.requestType = 'VACATION' " +
            "AND r.timeOffStart <= :end AND r.timeOffEnd >= :start")
    boolean hasApprovedVacation(@Param("userId") UUID userId,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);
}