package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
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
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) {
            return "redirect:/login";
        }

        try {
            userService.createUser(name, email, isAssigner, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Colaborador adicionado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao criar colaborador: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // --- NOVO: ENDPOINT PARA EDITAR O COLABORADOR ---
    @PostMapping("/{id}/update")
    public String updateUser(@PathVariable UUID id,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam(required = false) boolean isAssigner,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            userService.updateUserDetails(id, name, email, isAssigner, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Dados do colaborador atualizados com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao atualizar colaborador: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        boolean sucesso = userService.toggleUserStatus(id, adminActor);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Estado do colaborador alterado com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Não foi possível encontrar o colaborador.");
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/toggle-role")
    public String toggleUserRole(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        boolean sucesso = userService.toggleUserRole(id, adminActor);
        if (sucesso) {
            redirectAttributes.addFlashAttribute("successMsg", "Permissões do colaborador atualizadas com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Não foi possível encontrar o colaborador.");
        }

        return "redirect:/dashboard";
    }

    // =========================================================================
    // ROTA DA MATRIZ DE ELEGIBILIDADE
    // =========================================================================
    @PostMapping("/{id}/eligibility")
    public String updateEligibility(@PathVariable UUID id,
                                    @RequestParam(required = false) List<UUID> turnTypeIds,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        // Se o Gestor desmarcar todas as opções, previne NullPointerException
        if (turnTypeIds == null) {
            turnTypeIds = new ArrayList<>();
        }

        try {
            boolean sucesso = userService.updateEligibility(id, turnTypeIds, adminActor);
            if (sucesso) {
                redirectAttributes.addFlashAttribute("successMsg", "Matriz de elegibilidade atualizada com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "Não foi possível encontrar o colaborador.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao atualizar a matriz: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}