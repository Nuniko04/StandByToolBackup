package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.ScheduleGeneratorService;
import pt.sequoia.standByTool.services.UserService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleGeneratorService scheduleGeneratorService;
    private final UserService userService;

    public ScheduleController(ScheduleGeneratorService scheduleGeneratorService, UserService userService) {
        this.scheduleGeneratorService = scheduleGeneratorService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSchedule(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @AuthenticationPrincipal OidcUser principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

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