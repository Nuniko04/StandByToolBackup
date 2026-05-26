package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TipoFeriado;
import pt.sequoia.standByTool.services.FeriadoService;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/feriados")
public class FeriadoController {

    private final FeriadoService feriadoService;

    public FeriadoController(FeriadoService feriadoService) {
        this.feriadoService = feriadoService;
    }

    @PostMapping("/save")
    public String saveFeriado(@RequestParam String data,
                              @RequestParam String nome,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalDate localDate = LocalDate.parse(data);

            feriadoService.createFeriado(localDate, nome, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Holiday registered successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while registering holiday: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/update")
    public String updateFeriado(@PathVariable UUID id,
                                @RequestParam String data,
                                @RequestParam String nome,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalDate localDate = LocalDate.parse(data);

            feriadoService.updateFeriado(id, localDate, nome, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Holiday updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while updating holiday: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String deleteFeriado(@PathVariable UUID id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            feriadoService.deleteFeriado(id, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Holiday deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while deleting holiday: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/import")
    public String importFeriados(@RequestParam int ano,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            feriadoService.importarFeriados(ano);
            redirectAttributes.addFlashAttribute("successMsg", "Holidays from the year " + ano + " successfully imported!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while importing holidays: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}