package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TipoFeriado;
import pt.sequoia.standByTool.services.FeriadoService;
import pt.sequoia.standByTool.services.UserService;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/feriados")
public class FeriadoController {

    private final FeriadoService feriadoService;
    private final UserService userService;

    public FeriadoController(FeriadoService feriadoService, UserService userService) {
        this.feriadoService = feriadoService;
        this.userService = userService;
    }

    @PostMapping("/save")
    public String saveFeriado(@RequestParam String data,
                              @RequestParam String nome,
                              @AuthenticationPrincipal OidcUser principal,
                              RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

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
                                @AuthenticationPrincipal OidcUser principal,
                                RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

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
                                @AuthenticationPrincipal OidcUser principal,
                                RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

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
                                 @AuthenticationPrincipal OidcUser principal,
                                 RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        try {
            feriadoService.importarFeriados(ano);
            redirectAttributes.addFlashAttribute("successMsg", "Holidays from the year " + ano + " successfully imported!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while importing holidays: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}