package pt.sequoia.standByTool.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.services.RequestService;
import pt.sequoia.standByTool.services.UserService;

import java.util.UUID;

@Controller
@RequestMapping("/requests") // Removido o /api
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;

    public RequestController(RequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    // ==========================================
    // 1. COLABORADOR PEDE UMA TROCA (SWAP)
    // ==========================================
    @PostMapping("/swap")
    public String requestSwap(@RequestParam UUID turnId,
                              @RequestParam UUID targetUserId,
                              @AuthenticationPrincipal OidcUser principal,
                              RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User loggedUser = userService.findByEmail(email).orElse(null);

        try {
            // Nota padrão, já que o modal não pede texto justificativo
            String note = "Pedido de troca submetido via Dashboard";

            // Requer que o teu RequestService tenha o método atualizado que recebe o targetUserId
            requestService.createSwapRequest(loggedUser.getId(), turnId, targetUserId, note);

            redirectAttributes.addFlashAttribute("successMsg", "Swap request sent for approval!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while submitting request: " + e.getMessage());
        }

        if(loggedUser.isAssigner()){
            return "redirect:/employee-view";
        }
        return "redirect:/dashboard";
    }

    // ==========================================
    // 2. ASSIGNER APROVA A TROCA
    // ==========================================
    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User assigner = userService.findByEmail(email).orElse(null);

        try {
            // O serviço deve alterar o dono do turno e passar o Request para APPROVED
            requestService.processRequest(id, assigner.getId(), RequestStatus.APPROVED, "Approved via dashboard", null);
            redirectAttributes.addFlashAttribute("successMsg", "Request approved successfully! Turn transferred.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while approving request: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // ==========================================
    // 3. ASSIGNER REJEITA A TROCA
    // ==========================================
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable UUID id, @AuthenticationPrincipal OidcUser principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        // O Google garante que o email vem sempre
        String email = principal.getEmail();

        // Vais buscar à BD o utilizador completo (com as roles)
        User assigner = userService.findByEmail(email).orElse(null);

        try {
            // 💡 A CORREÇÃO ESTÁ AQUI: Passamos REJECTED em vez de DENIED
            requestService.processRequest(id, assigner.getId(), RequestStatus.DENIED, "Rejected via Dashboard", null);
            redirectAttributes.addFlashAttribute("successMsg", "Request rejected. Turn keeps its original worker.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error rejecting the request: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}