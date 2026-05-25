package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import pt.sequoia.standByTool.models.Feriado;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TipoFeriado;
import pt.sequoia.standByTool.repositories.FeriadoRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;
    private final AuditLogService auditLogService; // Injetado para manter o padrão do sistema
    private final RestTemplate restTemplate;

    public FeriadoService(FeriadoRepository feriadoRepository, AuditLogService auditLogService) {
        this.feriadoRepository = feriadoRepository;
        this.auditLogService = auditLogService;
        this.restTemplate = new RestTemplate();
    }

    // --- NOVOS MÉTODOS CRUD ---

    public List<Feriado> getAllFeriados() {
        return feriadoRepository.findAll();
    }

    public Optional<Feriado> getFeriadoById(UUID id) {
        return feriadoRepository.findById(id);
    }

    @Transactional
    public void createFeriado(LocalDate data, String nome, User adminActor) {
        if (feriadoRepository.existsByData(data)) {
            throw new IllegalArgumentException("Já existe um feriado registado nesta data.");
        }

        Feriado feriado = new Feriado();
        feriado.setData(data);
        feriado.setNome(nome);

        feriadoRepository.save(feriado);

        // Log de auditoria
        auditLogService.log(adminActor, "CREATE_FERIADO", "Feriado", feriado.getId(),
                "Feriado criado manualmente: " + nome + " em " + data);
    }

    @Transactional
    public void updateFeriado(UUID id, LocalDate data, String nome, User adminActor) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feriado não encontrado."));

        // Se a data foi alterada, garante que não choca com outro dia já registado
        if (!feriado.getData().equals(data) && feriadoRepository.existsByData(data)) {
            throw new IllegalArgumentException("Já existe um feriado registado nesta nova data.");
        }

        feriado.setData(data);
        feriado.setNome(nome);

        feriadoRepository.save(feriado);

        // Log de auditoria
        auditLogService.log(adminActor, "UPDATE_FERIADO", "Feriado", feriado.getId(),
                "Feriado atualizado: " + nome + " em " + data);
    }

    @Transactional
    public void deleteFeriado(UUID id, User adminActor) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feriado não encontrado."));

        feriadoRepository.delete(feriado);

        // Log de auditoria
        auditLogService.log(adminActor, "DELETE_FERIADO", "Feriado", id,
                "Feriado eliminado: " + feriado.getNome());
    }

    // --- MÉTODO DE IMPORTAÇÃO AUTOMÁTICA PRESERVADO ---
    public void importarFeriados(int ano) {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + ano + "/PT";
        try {
            FeriadoDTO[] feriadosAPI = restTemplate.getForObject(url, FeriadoDTO[].class);
            if (feriadosAPI != null) {
                for (FeriadoDTO dto : feriadosAPI) {
                    Feriado feriado = new Feriado();
                    feriado.setData(dto.getDate());
                    feriado.setNome(dto.getLocalName());

                    DayOfWeek diaDaSemana = dto.getDate().getDayOfWeek();
                    boolean isFimDeSemana = (diaDaSemana == DayOfWeek.SATURDAY || diaDaSemana == DayOfWeek.SUNDAY);
                    feriado.setBillable(!isFimDeSemana);
                    feriado.setTipo(TipoFeriado.ATIVO);

                    if (!feriadoRepository.existsByData(feriado.getData())) {
                        feriadoRepository.save(feriado);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao importar feriados: " + e.getMessage());
        }
    }

    private static class FeriadoDTO {
        private LocalDate date;
        private String localName;
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getLocalName() { return localName; }
        public void setLocalName(String localName) { this.localName = localName; }
    }
}