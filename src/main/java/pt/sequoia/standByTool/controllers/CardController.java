package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.sequoia.standByTool.models.Card;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.CardService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/api")
    @ResponseBody
    public List<Card> getAllCardsApi() {
        return cardService.getAllCards();
    }

    @PostMapping("/save")
    public String saveCard(@RequestParam String identifier,
                           @RequestParam String expirationDate,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

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
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User adminActor = (User) session.getAttribute("loggedUser");
        if (adminActor == null || !adminActor.isAssigner()) return "redirect:/login";

        try {
            cardService.updateCard(id, identifier, expirationDate, adminActor);
            redirectAttributes.addFlashAttribute("successMsg", "Card updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Error while updating card: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}