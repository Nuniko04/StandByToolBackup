package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnTypeService;

import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/turntypes")
public class TurnTypeController {

    private final TurnTypeService turnTypeService;

    public TurnTypeController(TurnTypeService turnTypeService) {
        this.turnTypeService = turnTypeService;
    }

    @PostMapping("/save")
    public String saveTurnType(@RequestParam String name,
                               @RequestParam(required = false) String googleCalendarId,
                               @RequestParam String color,
                               @RequestParam String defaultStartTime,
                               @RequestParam String defaultEndTime,
                               @RequestParam(required = false, defaultValue = "false") boolean eligibleForAutoGeneration,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalTime start = LocalTime.parse(defaultStartTime);
            LocalTime end = LocalTime.parse(defaultEndTime);

            turnTypeService.createTurnType(name, googleCalendarId, start, end, color, eligibleForAutoGeneration, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Turn type created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error creating turn type: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // --- NOVO: ENDPOINT PARA EDITAR O TIPO DE TURNO ---
    @PostMapping("/{id}/update")
    public String updateTurnType(@PathVariable UUID id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String googleCalendarId,
                                 @RequestParam String defaultStartTime,
                                 @RequestParam String defaultEndTime,
                                 @RequestParam(required = false, defaultValue = "#3498db") String color,
                                 @RequestParam(required = false, defaultValue = "false") boolean eligibleForAutoGeneration,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalTime start = LocalTime.parse(defaultStartTime);
            LocalTime end = LocalTime.parse(defaultEndTime);

            turnTypeService.updateTurnType(id, name, googleCalendarId, start, end, color, eligibleForAutoGeneration, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Turn type updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error updating turn type: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable UUID id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes){

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            turnTypeService.toggleStatus(id, adminActor);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error toggling turn type status: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}