package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.services.RequestService;

import java.util.UUID;

@Controller
@RequestMapping("/requests") // Removido o /api
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // ==========================================
    // 1. COLABORADOR PEDE UMA TROCA (SWAP)
    // ==========================================
    @PostMapping("/swap")
    public String requestSwap(@RequestParam UUID turnId,
                              @RequestParam UUID targetUserId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/login";

        try {
            // Nota padrão, já que o modal não pede texto justificativo
            String note = "Pedido de troca submetido via Dashboard";

            // Requer que o teu RequestService tenha o método atualizado que recebe o targetUserId
            requestService.createSwapRequest(loggedUser.getId(), turnId, targetUserId, note);

            redirectAttributes.addFlashAttribute("successMsg", "Pedido de troca enviado para aprovação!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao submeter troca: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // ==========================================
    // 2. ASSIGNER APROVA A TROCA
    // ==========================================
    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            // O serviço deve alterar o dono do turno e passar o Request para APPROVED
            requestService.processRequest(id, assigner.getId(), RequestStatus.APPROVED, "Aprovado via Dashboard", null);
            redirectAttributes.addFlashAttribute("successMsg", "Pedido aprovado com sucesso! O turno foi transferido.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao aprovar pedido: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // ==========================================
    // 3. ASSIGNER REJEITA A TROCA
    // ==========================================
    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable UUID id, HttpSession session, RedirectAttributes redirectAttributes) {
        User assigner = (User) session.getAttribute("loggedUser");
        if (assigner == null || !assigner.isAssigner()) return "redirect:/login";

        try {
            // 💡 A CORREÇÃO ESTÁ AQUI: Passamos REJECTED em vez de DENIED
            requestService.processRequest(id, assigner.getId(), RequestStatus.DENIED, "Rejeitado via Dashboard", null);
            redirectAttributes.addFlashAttribute("successMsg", "Pedido rejeitado. O turno mantém-se com o titular original.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao rejeitar pedido: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}