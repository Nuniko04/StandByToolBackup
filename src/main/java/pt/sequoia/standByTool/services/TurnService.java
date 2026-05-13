package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class TurnService {

    private final TurnRepository turnRepository;
    private final CalendarService calendarService;

    public TurnService(TurnRepository turnRepository, CalendarService calendarService) {
        this.turnRepository = turnRepository;
        this.calendarService = calendarService;
    }

    @Transactional
    public boolean acceptTurn(UUID turnId) {
        Optional<Turn> turnOpt = turnRepository.findById(turnId);

        if (turnOpt.isPresent()) {
            Turn turn = turnOpt.get();
            turn.setTurnStatus(TurnStatus.ACCEPTED); // Muda estado

            // Sincroniza com a Google
            String eventId = calendarService.addTurnToCalendar(turn);
            if (eventId != null) {
                turn.setCalendarEventId(eventId);
                System.out.println("✅ Evento adicionado ao Calendário: " + eventId);
            }

            turnRepository.save(turn);
            return true;
        }
        return false;
    }
}