package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/turns") // Removi o "/api", fica mais limpo para views
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @PostMapping("/assign")
    public String assignManualTurn(@RequestParam UUID assigneeId,
                                   @RequestParam UUID turnTypeId,
                                   @RequestParam String startDate,
                                   @RequestParam String endDate,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            turnService.createManualTurn(assigneeId, turnTypeId, LocalDateTime.parse(startDate), LocalDateTime.parse(endDate), assigner);
            redirectAttributes.addFlashAttribute("successMsg", "Turno manual criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao criar turno: " + e.getMessage());
        }

        return "redirect:/dashboard"; // Recarrega a página principal via GET
    }

    @PostMapping("/{id}/accept")
    public String acceptTurn(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        // Se não estiver logado, manda para o login
        if (loggedUser == null) {
            return "redirect:/login";
        }

        boolean sucesso = turnService.acceptTurn(id, loggedUser);

        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Turno aceite com sucesso e sincronizado no calendário!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao aceitar o turno. O turno pode já não estar pendente.");
        }

        // Volta para o dashboard do utilizador
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/update")
    public String updateTurn(@PathVariable UUID id,
                             @RequestParam(required = false) UUID assigneeId,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            LocalDateTime start = (startDate != null && !startDate.isBlank()) ? LocalDateTime.parse(startDate) : null;
            LocalDateTime end = (endDate != null && !endDate.isBlank()) ? LocalDateTime.parse(endDate) : null;

            turnService.updateTurn(id, assigneeId, start, end, assigner);
            redirectAttributes.addFlashAttribute("successMsg", "Turno atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao atualizar turno: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String deleteTurn(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        boolean sucesso = turnService.deleteTurn(id, assigner);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Turno apagado!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Turno não encontrado.");
        }

        return "redirect:/dashboard";
    }
}