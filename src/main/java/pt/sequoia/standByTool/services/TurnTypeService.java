package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.TurnTypeRepository;

import java.time.LocalTime;
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

    public List<TurnType> getAllTurnTypes() {
        return turnTypeRepository.findAll();
    }

    public Optional<TurnType> findById(UUID id) {
        return turnTypeRepository.findById(id);
    }

    @Transactional
    public void createTurnType(String name, String googleCalendarId, LocalTime start, LocalTime end, String color, boolean autoGenerate, User adminActor) {
        TurnType turnType = new TurnType();
        turnType.setName(name);
        turnType.setGoogleCalendarId(googleCalendarId);
        turnType.setDefaultStartTime(start);
        turnType.setDefaultEndTime(end);
        turnType.setColor(color);
        turnType.setEligibleForAutoGeneration(autoGenerate);

        turnTypeRepository.save(turnType);
        auditLogService.log(adminActor, "CREATE_TURN_TYPE", "TurnType", turnType.getId(), "Created TurnType: " + name);
    }

    // --- NOVO: MÉTODO PARA EDITAR TIPO DE TURNO ---
    @Transactional
    public void updateTurnType(UUID id, String name, String googleCalendarId, LocalTime start, LocalTime end, String color, boolean autoGenerate, User adminActor) {
        TurnType turnType = turnTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de Turno não encontrado."));

        turnType.setName(name);
        turnType.setGoogleCalendarId(googleCalendarId);
        turnType.setDefaultStartTime(start);
        turnType.setDefaultEndTime(end);
        turnType.setColor(color);
        turnType.setEligibleForAutoGeneration(autoGenerate);

        turnTypeRepository.save(turnType);
        auditLogService.log(adminActor, "UPDATE_TURN_TYPE", "TurnType", turnType.getId(), "Updated TurnType: " + name);
    }

    @Transactional
    public void toggleStatus(UUID id, User adminActor) {
        TurnType turnType = turnTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de Turno não encontrado."));

        turnType.setDeleted(!turnType.isDeleted());

        turnTypeRepository.save(turnType);
        auditLogService.log(adminActor, "TOGGLE_TURN_TYPE_STATUS", "TurnType", turnType.getId(), "Toggling TurnType status: " + turnType.getName());
    }
}