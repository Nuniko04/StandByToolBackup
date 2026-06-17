package pt.sequoia.standByTool.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
    private final RestTemplate restTemplate;

    public FeriadoService(FeriadoRepository feriadoRepository) {
        this.feriadoRepository = feriadoRepository;
        this.restTemplate = new RestTemplate();
    }

    // --- NOVOS MÉTODOS CRUD ---

    public List<Feriado> getAllFeriados() {
        return feriadoRepository.findAllOrdered();
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
    }

    @Transactional
    public void deleteFeriado(UUID id, User adminActor) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feriado não encontrado."));

        feriadoRepository.delete(feriado);
    }

    public void importarFeriados(int ano) {
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + ano + "/PT";

        try {
            ResponseEntity<List<FeriadoDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<FeriadoDTO>>() {
                    }
            );

            List<FeriadoDTO> feriadosDaApi = response.getBody();

            if (feriadosDaApi != null) {
                for (FeriadoDTO dto : feriadosDaApi) {

                    // 💡 O TRUQUE ESTÁ AQUI: Filtrar por tipo!
                    // A API Nager define feriados nacionais (Public) ou locais/regionais (Authorities, Optional, etc).
                    // Só queremos os "Public" (Nacionais) e garantir que não tem a flag "counties" preenchida (que indica regionalismo).

                    boolean isNacional = dto.getTypes().contains("Public") && dto.getCounties() == null;

                    if (isNacional) {
                        boolean jaExiste = feriadoRepository.existsByData(dto.getDate());

                        if (!jaExiste) {
                            Feriado feriado = new Feriado();
                            feriado.setNome(dto.getLocalName());
                            feriado.setData(dto.getDate());

                            feriadoRepository.save(feriado);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao importar feriados da API: " + e.getMessage());
        }
    }

    private static class FeriadoDTO {
        private LocalDate date;
        private String localName;
        private List<String> types;    // 💡 NOVO: Para ler o "Public"
        private List<String> counties; // 💡 NOVO: Para ler a região

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getLocalName() { return localName; }
        public void setLocalName(String localName) { this.localName = localName; }

        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }

        public List<String> getCounties() { return counties; }
        public void setCounties(List<String> counties) { this.counties = counties; }
    }
}