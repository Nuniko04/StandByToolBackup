package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.TurnTypeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TurnTypeService {

    private final TurnTypeRepository turnTypeRepository;
    private final AuditLogService auditLogService;

    public TurnTypeService(TurnTypeRepository turnTypeRepository, AuditLogService auditLogService) {
        this.turnTypeRepository = turnTypeRepository;
        this.auditLogService = auditLogService;
    }

    public List<TurnType> getAllTurnTypes() { return turnTypeRepository.findAll(); }

    @Transactional
    public boolean updateTurnType(UUID id, String name, BigDecimal defaultValue, String googleCalendarId, User adminActor) {
        Optional<TurnType> opt = turnTypeRepository.findById(id);

        if (opt.isPresent()) {
            TurnType turnType = opt.get();
            if (name != null && !name.isBlank()) turnType.setName(name);
            if (defaultValue != null) turnType.setDefaultValue(defaultValue);
            if (googleCalendarId != null && !googleCalendarId.isBlank()) turnType.setGoogleCalendarId(googleCalendarId);

            turnTypeRepository.save(turnType);
            auditLogService.log(adminActor, "UPDATE_TURN_TYPE", "TurnType", id, "Settings updated for: " + turnType.getName());
            return true;
        }
        return false;
    }
}