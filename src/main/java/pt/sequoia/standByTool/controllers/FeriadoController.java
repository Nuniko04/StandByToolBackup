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
                              @RequestParam String tipo,
                              @RequestParam(required = false, defaultValue = "false") boolean billable,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalDate localDate = LocalDate.parse(data);
            TipoFeriado tipoFeriado = TipoFeriado.valueOf(tipo);

            feriadoService.createFeriado(localDate, nome, tipoFeriado, billable, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Feriado registado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao registar feriado: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/update")
    public String updateFeriado(@PathVariable UUID id,
                                @RequestParam String data,
                                @RequestParam String nome,
                                @RequestParam String tipo,
                                @RequestParam(required = false, defaultValue = "false") boolean billable,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            LocalDate localDate = LocalDate.parse(data);
            TipoFeriado tipoFeriado = TipoFeriado.valueOf(tipo);

            feriadoService.updateFeriado(id, localDate, nome, tipoFeriado, billable, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Feriado atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao atualizar feriado: " + e.getMessage());
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
            redirectAttributes.addFlashAttribute("successMsg", "Feriado removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao remover feriado: " + e.getMessage());
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
            redirectAttributes.addFlashAttribute("successMsg", "Feriados do ano " + ano + " importados com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao importar feriados: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}