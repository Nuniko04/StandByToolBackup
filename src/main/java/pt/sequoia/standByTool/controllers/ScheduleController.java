package pt.sequoia.standByTool.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.sequoia.standByTool.services.ScheduleGeneratorService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleGeneratorService scheduleGeneratorService;

    public ScheduleController(ScheduleGeneratorService scheduleGeneratorService) {
        this.scheduleGeneratorService = scheduleGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<String>> generateSchedule(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Chama o nosso algoritmo
        List<String> alertas = scheduleGeneratorService.gerarEscalas(start, end);

        // Devolve os alertas gerados para o Frontend mostrar nos Pop-ups
        return ResponseEntity.ok(alertas);
    }
}