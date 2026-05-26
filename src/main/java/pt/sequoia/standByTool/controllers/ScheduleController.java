package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.ScheduleGeneratorService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleGeneratorService scheduleGeneratorService;

    public ScheduleController(ScheduleGeneratorService scheduleGeneratorService) {
        this.scheduleGeneratorService = scheduleGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSchedule(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            HttpSession session) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) {
            return ResponseEntity.status(403).body("Access Denied. Only Assigners can generate the schedule.");
        }

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Chama o nosso algoritmo passando o utilizador logado para o AuditLog
        List<String> alertas = scheduleGeneratorService.gerarEscalas(start, end, adminActor);

        return ResponseEntity.ok(alertas);
    }
}