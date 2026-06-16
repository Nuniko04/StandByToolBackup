package pt.sequoia.standByTool.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnService;
import pt.sequoia.standByTool.services.TurnTypeService;
import pt.sequoia.standByTool.services.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/turns")
public class TurnController {

    private final TurnService turnService;
    private final TurnTypeService turnTypeService;
    private final UserService userService;

    public TurnController(TurnService turnService, TurnTypeService turnTypeService, UserService userService) {
        this.turnService = turnService;
        this.turnTypeService = turnTypeService;
        this.userService = userService;
    }

    @PostMapping("/assign")
    public String assignManualTurn(@RequestParam UUID assigneeId,
                                   @RequestParam UUID turnTypeId,
                                   @RequestParam(required = false) UUID cardId, // NOVO PARÂMETRO
                                   @RequestParam String startDate,
                                   @RequestParam String endDate,
                                   @AuthenticationPrincipal OidcUser principal,
                                   RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User assigner = userService.findByEmail(email).orElse(null);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            TurnType type = turnTypeService.findById(turnTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Turn type not found"));

            LocalTime sTime = type.getDefaultStartTime() != null ? type.getDefaultStartTime() : LocalTime.MIDNIGHT;
            LocalTime eTime = type.getDefaultEndTime() != null ? type.getDefaultEndTime() : LocalTime.MIDNIGHT;

            LocalDateTime finalStart = start.atTime(sTime);
            LocalDateTime finalEnd = end.atTime(eTime);

            // Passamos o cardId para o serviço
            turnService.createManualTurn(assigneeId, turnTypeId, cardId, finalStart, finalEnd, assigner);

            redirectAttributes.addFlashAttribute("successMsg", "Turn created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while creating turn: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/accept")
    public String acceptTurn(@PathVariable UUID id, jakarta.servlet.http.HttpServletRequest request, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User loggedUser = userService.findByEmail(email).orElse(null);

        boolean sucesso = turnService.acceptTurn(id, loggedUser);

        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Turn accepted successfully and synchronized in the calendar!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Error accepting turn. Turn can already be not pending.");
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @PostMapping("/{id}/update")
    public String updateTurn(@PathVariable UUID id,
                             @RequestParam(required = false) UUID assigneeId,
                             @RequestParam(required = false) UUID cardId, // NOVO PARÂMETRO
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @AuthenticationPrincipal OidcUser principal,
                             RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User assigner = userService.findByEmail(email).orElse(null);

        try {
            Turn turn = turnService.getTurn(id)
                    .orElseThrow(() -> new IllegalArgumentException("Turn not found"));

            TurnType type = turn.getTurnType();

            LocalTime sTime = type.getDefaultStartTime() != null ? type.getDefaultStartTime() : LocalTime.MIDNIGHT;
            LocalTime eTime = type.getDefaultEndTime() != null ? type.getDefaultEndTime() : LocalTime.MIDNIGHT;

            LocalDateTime start = null;
            if (startDate != null && !startDate.isBlank()) {
                start = LocalDate.parse(startDate).atTime(sTime);
            }

            LocalDateTime end = null;
            if (endDate != null && !endDate.isBlank()) {
                end = LocalDate.parse(endDate).atTime(eTime);
            }

            // Passamos o cardId para o serviço atualizar
            turnService.updateTurn(id, assigneeId, cardId, start, end, assigner);

            redirectAttributes.addFlashAttribute("successMsg", "Turn updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error updating turn: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String deleteTurn(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User assigner = userService.findByEmail(email).orElse(null);

        boolean sucesso = turnService.deleteTurn(id, assigner);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Turn deleted!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Turn not found.");
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/accept-all")
    public String acceptAllTurns(jakarta.servlet.http.HttpServletRequest request, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User loggedUser = userService.findByEmail(email).orElse(null);

        try {
            int count = turnService.acceptAllPendingTurns(loggedUser);
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMsg", count + " turn(s) accepted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "No pending turns to accept");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while accepting turns: " + e.getMessage());
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}