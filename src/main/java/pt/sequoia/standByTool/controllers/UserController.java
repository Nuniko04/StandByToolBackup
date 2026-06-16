package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api")
    @ResponseBody
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/save")
    public String saveUser(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam(required = false) boolean isAssigner,
                           @AuthenticationPrincipal OidcUser principal,
                           RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String emailG = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(emailG).orElse(null);

        if (adminActor == null || !adminActor.isAssigner()) {
            return "redirect:/login";
        }

        try {
            userService.createUser(name, email, isAssigner, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Worker added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while adding worker: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // --- NOVO: ENDPOINT PARA EDITAR O COLABORADOR ---
    @PostMapping("/{id}/update")
    public String updateUser(@PathVariable UUID id,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) boolean isAssigner,
                             @AuthenticationPrincipal OidcUser principal,
                             RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String emailG = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(emailG).orElse(null);

        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            userService.updateUserDetails(id, name, email, isAssigner, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Worker data updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error updating worker data: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        boolean sucesso = userService.toggleUserStatus(id, adminActor);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Worker status updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found.");
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/toggle-role")
    public String toggleUserRole(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        boolean sucesso = userService.toggleUserRole(id, adminActor);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Worker permissions updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Worker not found.");
        }

        return "redirect:/dashboard";
    }

    // =========================================================================
    // ROTA DA MATRIZ DE ELEGIBILIDADE
    // =========================================================================
    @PostMapping("/{id}/eligibility")
    public String updateEligibility(@PathVariable UUID id,
                                    @RequestParam(required = false) List<UUID> turnTypeIds,
                                    @AuthenticationPrincipal OidcUser principal,
                                    RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User adminActor = userService.findByEmail(email).orElse(null);

        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        // Se o Gestor desmarcar todas as opções, previne NullPointerException
        if (turnTypeIds == null) {
            turnTypeIds = new ArrayList<>();
        }

        try {
            boolean sucesso = userService.updateEligibility(id, turnTypeIds, adminActor);
            if (sucesso) {
                redirectAttributes.addFlashAttribute("successMsg", "Eligibility matrix updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "Worker not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error updating matrix: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}