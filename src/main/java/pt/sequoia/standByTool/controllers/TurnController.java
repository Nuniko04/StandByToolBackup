package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.TurnService;
import pt.sequoia.standByTool.services.TurnTypeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/turns")
public class TurnController {

    private final TurnService turnService;
    private final TurnTypeService turnTypeService;

    public TurnController(TurnService turnService, TurnTypeService turnTypeService) {
        this.turnService = turnService;
        this.turnTypeService = turnTypeService;
    }

    @PostMapping("/assign")
    public String assignManualTurn(@RequestParam UUID assigneeId,
                                   @RequestParam UUID turnTypeId,
                                   @RequestParam(required = false) UUID cardId, // NOVO PARÂMETRO
                                   @RequestParam String startDate,
                                   @RequestParam String endDate,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            TurnType type = turnTypeService.findById(turnTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de turno não encontrado"));

            LocalTime sTime = type.getDefaultStartTime() != null ? type.getDefaultStartTime() : LocalTime.MIDNIGHT;
            LocalTime eTime = type.getDefaultEndTime() != null ? type.getDefaultEndTime() : LocalTime.MIDNIGHT;

            LocalDateTime finalStart = start.atTime(sTime);
            LocalDateTime finalEnd = end.atTime(eTime);

            // Passamos o cardId para o serviço
            turnService.createManualTurn(assigneeId, turnTypeId, cardId, finalStart, finalEnd, assigner);

            redirectAttributes.addFlashAttribute("successMsg", "Turno manual criado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao criar turno: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/accept")
    public String acceptTurn(@PathVariable UUID id, jakarta.servlet.http.HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        boolean sucesso = turnService.acceptTurn(id, loggedUser);

        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Turno aceite com sucesso e sincronizado no calendário!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao aceitar o turno. O turno pode já não estar pendente.");
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
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            Turn turn = turnService.getTurn(id)
                    .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado"));

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