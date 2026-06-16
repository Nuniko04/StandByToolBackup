package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.Card;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.CardService;
import pt.sequoia.standByTool.services.UserService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    public CardController(CardService cardService, UserService userService) {
        this.cardService = cardService;
        this.userService = userService;

    }

    @GetMapping("/api")
    @ResponseBody
    public List<Card> getAllCardsApi() {
        return cardService.getAllCards();
    }

    @PostMapping("/save")
    public String saveCard(@RequestParam String identifier,
                           @RequestParam String expirationDate,
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
            cardService.createCard(identifier, expirationDate, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Card registered successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while registering card: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // --- NOVO: ENDPOINT PARA EDITAR O CARTÃO ---
    @PostMapping("/{id}/update")
    public String updateCard(@PathVariable UUID id,
                             @RequestParam String identifier,
                             @RequestParam String expirationDate,
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
            cardService.updateCard(id, identifier, expirationDate, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Card updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while updating card: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}