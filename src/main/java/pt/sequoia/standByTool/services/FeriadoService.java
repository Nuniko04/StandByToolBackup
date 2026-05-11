package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pt.sequoia.standByTool.models.Feriado;
import pt.sequoia.standByTool.models.enums.TipoFeriado;
import pt.sequoia.standByTool.repositories.FeriadoRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;

@Service
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;
    private final RestTemplate restTemplate;

    public FeriadoService(FeriadoRepository feriadoRepository) {
        this.feriadoRepository = feriadoRepository;
        this.restTemplate = new RestTemplate(); // Spring Boot HTTP Client
    }

    public void importarFeriados(int ano) {
        // API Pública e Gratuita (Não precisa de API Key)
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + ano + "/PT";

        try {
            // Vai buscar o JSON e converte automaticamente para um Array de FeriadoDTO
            FeriadoDTO[] feriadosAPI = restTemplate.getForObject(url, FeriadoDTO[].class);

            if (feriadosAPI != null) {
                for (FeriadoDTO dto : feriadosAPI) {
                    Feriado feriado = new Feriado();
                    feriado.setData(dto.getDate());
                    feriado.setNome(dto.getLocalName());

                    // Lógica do Billable: Se for fim de semana, é falso. Se for dia de semana, é verdadeiro.
                    DayOfWeek diaDaSemana = dto.getDate().getDayOfWeek();
                    boolean isFimDeSemana = (diaDaSemana == DayOfWeek.SATURDAY || diaDaSemana == DayOfWeek.SUNDAY);
                    feriado.setBillable(!isFimDeSemana);

                    // Assume Nacional (Ativo) por defeito, o Admin pode mudar depois
                    feriado.setTipo(TipoFeriado.ATIVO);

                    // Só guarda se a data ainda não existir na BD
                    if (!feriadoRepository.existsByData(feriado.getData())) {
                        feriadoRepository.save(feriado);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao importar feriados: " + e.getMessage());
        }
    }

    // Classe DTO auxiliar (Data Transfer Object) para ler o JSON da API
    private static class FeriadoDTO {
        private LocalDate date;
        private String localName;
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getLocalName() { return localName; }
        public void setLocalName(String localName) { this.localName = localName; }
    }
}