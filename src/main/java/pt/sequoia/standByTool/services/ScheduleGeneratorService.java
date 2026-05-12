package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleGeneratorService {

    private final UserRepository userRepository;
    private final TurnRepository turnRepository;
    private final RequestRepository requestRepository;
    private final FeriadoRepository feriadoRepository;
    private final TurnTypeRepository turnTypeRepository;

    public ScheduleGeneratorService(UserRepository userRepository,
                                    TurnRepository turnRepository,
                                    RequestRepository requestRepository,
                                    FeriadoRepository feriadoRepository,
                                    TurnTypeRepository turnTypeRepository) {
        this.userRepository = userRepository;
        this.turnRepository = turnRepository;
        this.requestRepository = requestRepository;
        this.feriadoRepository = feriadoRepository;
        this.turnTypeRepository = turnTypeRepository;
    }

    // Classe interna para guardar a pontuação
    private static class ColaboradorScore {
        User user;
        int pontos = 0;
        public ColaboradorScore(User user) { this.user = user; }
    }

    @Transactional
    public List<String> gerarEscalas(LocalDate dataInicio, LocalDate dataFim) {

        List<String> alertasGerados = new ArrayList<>();
        LocalDate semanaAtual = dataInicio;

        // Vai buscar os tipos de turno à BD (Assumindo que os nomes são "StandBy" e "Backup")
        TurnType tipoStandBy = turnTypeRepository.findByName("StandBy");
        TurnType tipoBackup = turnTypeRepository.findByName("Backup");
        TurnType tipoFinastraShift = turnTypeRepository.findByName("Finastra Shift");

        while (semanaAtual.isBefore(dataFim)) {
            LocalDate inicioSemana = semanaAtual;
            LocalDate fimSemana = semanaAtual.plusDays(6);

            // =====================================================================
            // CONVERSÃO MÁGICA: De LocalDate (Sem hora) para OffsetDateTime (Com hora)
            // Começa à meia-noite de Segunda, acaba às 23:59:59 de Domingo
            // =====================================================================
            OffsetDateTime inicioComHora = inicioSemana.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime fimComHora = fimSemana.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            OffsetDateTime proxSemanaInicio = inicioComHora.plusDays(7);
            OffsetDateTime proxSemanaFim = fimComHora.plusDays(7);

            List<User> todosColaboradores = userRepository.findAllActiveEmployees();
            List<ColaboradorScore> candidatos = new ArrayList<>();

            // 1. FASE DE EXCLUSÃO (Regras Rígidas)
            for (User user : todosColaboradores) {
                boolean feriasNaSemana = requestRepository.hasApprovedVacation(user.getId(), inicioComHora, fimComHora);
                boolean feriasProximaSemana = requestRepository.hasApprovedVacation(user.getId(), proxSemanaInicio, proxSemanaFim);
                boolean temTurno = turnRepository.existsByAssigneeAndDates(user.getId(), inicioComHora, fimComHora);

                if (!feriasNaSemana && !feriasProximaSemana && !temTurno) {
                    candidatos.add(new ColaboradorScore(user));
                }
            }

            // 2. FASE DE SCORING (Equidade e Cadência)
            boolean isSemanaComFeriado = feriadoRepository.existsFeriadoInPeriod(inicioSemana, fimSemana);
            boolean isSemanaFechoMes = checkFechoDeMes(inicioSemana, fimSemana);

            for (ColaboradorScore candidato : candidatos) {

                // Regra: Cadência
                Integer semanasDb = turnRepository.getWeeksSinceLastTurn(candidato.user.getId(), inicioComHora);
                int semanasDesdeUltimoTurno = (semanasDb != null) ? semanasDb : 10;
                candidato.pontos += (semanasDesdeUltimoTurno * 10) - 40;

                // Regra: Feriados
                if (isSemanaComFeriado) {
                    int feriadosTrabalhados = turnRepository.countFeriadosTrabalhados(candidato.user.getId(), inicioSemana.getYear());
                    candidato.pontos -= (feriadosTrabalhados * 50);
                }

                // Regra: Fecho de Mês
                if (isSemanaFechoMes) {
                    int fechosTrabalhados = turnRepository.countFechosMesTrabalhados(candidato.user.getId(), inicioSemana.getYear());
                    candidato.pontos -= (fechosTrabalhados * 30);
                }
            }

            // 3. ORDENAÇÃO E ATRIBUIÇÃO NA BASE DE DADOS

            ZoneId zoneLisboa = ZoneId.of("Europe/Lisbon");
            boolean isHoraDeVerao = zoneLisboa.getRules().isDaylightSavings(inicioComHora.toInstant());

            int recursosNecessarios = isHoraDeVerao ? 3 : 2;

            if (candidatos.size() >= recursosNecessarios) {
                // Ordena os candidatos: Quem tem MAIOR pontuação fica em primeiro
                candidatos.sort((a, b) -> Integer.compare(b.pontos, a.pontos));

                User melhorParaStandBy = candidatos.get(0).user;
                User melhorParaBackup = candidatos.get(1).user;
                User melhorParaShift = null;

                // Se for Hora de Verão, vamos procurar o melhor candidato ELEGÍVEL para o Finastra
                if (isHoraDeVerao) {
                    for (int i = 2; i < candidatos.size(); i++) {
                        if (candidatos.get(i).user.isFinastraEligible()) {
                            melhorParaShift = candidatos.get(i).user;
                            break; // Encontrámos! Pára a procura.
                        }
                    }
                }

                // VALIDAÇÃO FINAL: Se for hora de verão mas ninguém na lista podia fazer Finastra
                if (isHoraDeVerao && melhorParaShift == null) {
                    String aviso = "⚠️ Semana de " + inicioSemana + ": Existem pessoas disponíveis, mas NENHUMA tem a permissão 'Finastra Shift'. Intervenção manual necessária!";
                    alertasGerados.add(aviso);
                } else {
                    // --- Gravar o StandBy ---
                    Turn turnoStandBy = new Turn();
                    turnoStandBy.setAssignee(melhorParaStandBy);
                    turnoStandBy.setTurnType(tipoStandBy);
                    turnoStandBy.setStartTime(inicioComHora);
                    turnoStandBy.setEndTime(fimComHora);
                    turnoStandBy.setTurnValue(BigDecimal.ZERO);
                    turnoStandBy.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                    turnRepository.save(turnoStandBy);

                    // --- Gravar o Backup ---
                    Turn turnoBackup = new Turn();
                    turnoBackup.setAssignee(melhorParaBackup);
                    turnoBackup.setTurnType(tipoBackup);
                    turnoBackup.setStartTime(inicioComHora);
                    turnoBackup.setEndTime(fimComHora);
                    turnoBackup.setTurnValue(BigDecimal.ZERO);
                    turnoBackup.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                    turnRepository.save(turnoBackup);

                    // --- Gravar o Finastra Shift ---
                    if (isHoraDeVerao) {
                        Turn turnoShift = new Turn();
                        turnoShift.setAssignee(melhorParaShift);
                        turnoShift.setTurnType(tipoFinastraShift);
                        turnoShift.setStartTime(inicioComHora);
                        turnoShift.setEndTime(fimComHora);
                        turnoShift.setTurnValue(BigDecimal.ZERO);
                        turnoShift.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                        turnRepository.save(turnoShift);
                    }
                }

            } else {
                // Alerta de recursos insuficientes em número absoluto
                String aviso = "⚠️ Semana de " + inicioSemana + ": Recursos insuficientes (Precisava de " + recursosNecessarios + ", mas só há " + candidatos.size() + " disponíveis).";
                alertasGerados.add(aviso);
            }

            // Avança para a próxima semana
            semanaAtual = semanaAtual.plusDays(7);
        }

        return alertasGerados;
    }

    private boolean checkFechoDeMes(LocalDate inicio, LocalDate fim) {
        return inicio.getMonth() != fim.getMonth() || fim.getDayOfMonth() >= 28;
    }
}