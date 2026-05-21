package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Card;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.CardRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AuditLogService auditLogService;

    public CardService(CardRepository cardRepository, AuditLogService auditLogService) {
        this.cardRepository = cardRepository;
        this.auditLogService = auditLogService;
    }

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    @Transactional
    public void createCard(String identifier, String expirationDate, User adminActor) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("O identificador do cartão não pode estar vazio.");
        }
        if (expirationDate == null || expirationDate.isBlank()) {
            throw new IllegalArgumentException("A data de validade é obrigatória.");
        }

        Card card = new Card();
        card.setIdentifier(identifier);
        card.setExpirationDate(expirationDate);

        cardRepository.save(card);

        auditLogService.log(adminActor, "CREATE_CARD", "Card", card.getId(),
                "Card registered: " + identifier);
    }

    // --- NOVO: MÉTODO PARA EDITAR O CARTÃO ---
    @Transactional
    public void updateCard(UUID id, String identifier, String expirationDate, User adminActor) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("O identificador do cartão não pode estar vazio.");
        }

        card.setIdentifier(identifier);
        card.setExpirationDate(expirationDate); // A validade pode ser atualizada

        cardRepository.save(card);

        auditLogService.log(adminActor, "UPDATE_CARD", "Card", card.getId(),
                "Card updated to: " + identifier);
    }
}