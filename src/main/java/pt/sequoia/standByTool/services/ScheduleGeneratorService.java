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
    private final AuditLogService auditLogService; // <-- Novo serviço injetado

    public ScheduleGeneratorService(UserRepository userRepository,
                                    TurnRepository turnRepository,
                                    RequestRepository requestRepository,
                                    FeriadoRepository feriadoRepository,
                                    TurnTypeRepository turnTypeRepository,
                                    AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.turnRepository = turnRepository;
        this.requestRepository = requestRepository;
        this.feriadoRepository = feriadoRepository;
        this.turnTypeRepository = turnTypeRepository;
        this.auditLogService = auditLogService;
    }

    // Classe interna para guardar a pontuação
    private static class ColaboradorScore {
        User user;
        int pontos = 0;
        public ColaboradorScore(User user) { this.user = user; }
    }

    @Transactional
    public List<String> gerarEscalas(LocalDate dataInicio, LocalDate dataFim, User adminActor) { // <-- Recebe o adminActor

        List<String> alertasGerados = new ArrayList<>();
        LocalDate semanaAtual = dataInicio;

        TurnType tipoStandBy = turnTypeRepository.findByName("StandBy");
        TurnType tipoBackup = turnTypeRepository.findByName("Backup");
        TurnType tipoFinastraShift = turnTypeRepository.findByName("Finastra Shift");
        ZoneId zoneLisboa = ZoneId.of("Europe/Lisbon");

        while (semanaAtual.isBefore(dataFim)) {
            LocalDate inicioSemana = semanaAtual;
            LocalDate fimSemana = semanaAtual.plusDays(6);

            // 1. VERIFICAÇÃO INTELIGENTE DE "BURACOS"
            boolean isHoraDeVerao = zoneLisboa.getRules().isDaylightSavings(
                    inicioSemana.atStartOfDay().toInstant(ZoneOffset.UTC)
            );

            boolean faltaStandBy = !turnRepository.existsTurnOfTypeInWeek("StandBy", inicioSemana);
            boolean faltaBackup = !turnRepository.existsTurnOfTypeInWeek("Backup", inicioSemana);
            boolean faltaFinastra = isHoraDeVerao && !turnRepository.existsTurnOfTypeInWeek("Finastra Shift", inicioSemana);

            int recursosNecessarios = 0;
            if (faltaStandBy) recursosNecessarios++;
            if (faltaBackup) recursosNecessarios++;
            if (faltaFinastra) recursosNecessarios++;

            // Se não faltar nenhum turno, salta para a próxima semana!
            if (recursosNecessarios == 0) {
                alertasGerados.add("ℹ️ Semana de " + inicioSemana + " ignorada: Todas as escalas necessárias já estavam preenchidas.");
                semanaAtual = semanaAtual.plusDays(7);
                continue;
            }

            // Referências para a semana seguinte (usadas na verificação de férias)
            LocalDate proxSemanaInicio = inicioSemana.plusDays(7);
            LocalDate proxSemanaFim = fimSemana.plusDays(7);

            List<User> todosColaboradores = userRepository.findAllActiveEmployees();
            List<ColaboradorScore> candidatos = new ArrayList<>();

            // 2. FASE DE EXCLUSÃO
            for (User user : todosColaboradores) {
                boolean feriasNaSemana = requestRepository.hasApprovedVacation(user.getId(), inicioSemana, fimSemana);
                boolean feriasProximaSemana = requestRepository.hasApprovedVacation(user.getId(), proxSemanaInicio, proxSemanaFim);
                boolean temTurno = turnRepository.existsByAssigneeAndDates(user.getId(), inicioSemana, fimSemana);

                if (!feriasNaSemana && !feriasProximaSemana && !temTurno) {
                    candidatos.add(new ColaboradorScore(user));
                }
            }

            // 3. FASE DE SCORING (Equidade e Cadência)
            boolean isSemanaComFeriado = feriadoRepository.existsFeriadoInPeriod(inicioSemana, fimSemana);
            boolean isSemanaFechoMes = checkFechoDeMes(inicioSemana, fimSemana);

            for (ColaboradorScore candidato : candidatos) {
                Integer semanasDb = turnRepository.getWeeksSinceLastTurn(candidato.user.getId(), inicioSemana);
                int semanasDesdeUltimoTurno = (semanasDb != null) ? semanasDb : 10;
                candidato.pontos += (semanasDesdeUltimoTurno * 10) - 40;

                if (isSemanaComFeriado) {
                    int feriadosTrabalhados = turnRepository.countFeriadosTrabalhados(candidato.user.getId(), inicioSemana.getYear());
                    candidato.pontos -= (feriadosTrabalhados * 50);
                }

                if (isSemanaFechoMes) {
                    int fechosTrabalhados = turnRepository.countFechosMesTrabalhados(candidato.user.getId(), inicioSemana.getYear());
                    candidato.pontos -= (fechosTrabalhados * 30);
                }
            }

            // 4. ORDENAÇÃO E PREENCHIMENTO DOS BURACOS
            if (candidatos.size() >= recursosNecessarios) {
                // Ordena os candidatos do maior score para o menor
                candidatos.sort((a, b) -> Integer.compare(b.pontos, a.pontos));

                int indiceAtual = 0; // Vai avançando na lista conforme gastamos pessoas

                if (faltaStandBy) {
                    User melhorParaStandBy = candidatos.get(indiceAtual).user;

                    Turn turnoStandBy = new Turn();
                    turnoStandBy.setAssignee(melhorParaStandBy);
                    turnoStandBy.setTurnType(tipoStandBy);
                    turnoStandBy.setStartTime(inicioSemana);
                    turnoStandBy.setEndTime(inicioSemana.plusDays(6));
                    turnoStandBy.setTurnValue(BigDecimal.ZERO);
                    turnoStandBy.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                    turnRepository.save(turnoStandBy);

                    indiceAtual++; // Gastámos um candidato, passa ao próximo
                }

                if (faltaBackup) {
                    User melhorParaBackup = candidatos.get(indiceAtual).user;

                    Turn turnoBackup = new Turn();
                    turnoBackup.setAssignee(melhorParaBackup);
                    turnoBackup.setTurnType(tipoBackup);
                    turnoBackup.setStartTime(inicioSemana);
                    turnoBackup.setEndTime(inicioSemana.plusDays(6));
                    turnoBackup.setTurnValue(BigDecimal.ZERO);
                    turnoBackup.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                    turnRepository.save(turnoBackup);

                    indiceAtual++;
                }

                if (faltaFinastra) {
                    User melhorParaShift = null;
                    // Procura o melhor para Finastra APENAS entre os candidatos que sobraram
                    for (int i = indiceAtual; i < candidatos.size(); i++) {
                        if (candidatos.get(i).user.isFinastraEligible()) {
                            melhorParaShift = candidatos.get(i).user;
                            break;
                        }
                    }

                    if (melhorParaShift == null) {
                        alertasGerados.add("⚠️ Semana de " + inicioSemana + ": Ninguém com permissão 'Finastra Shift' disponível para preencher o turno em falta!");
                    } else {
                        Turn turnoShift = new Turn();
                        turnoShift.setAssignee(melhorParaShift);
                        turnoShift.setTurnType(tipoFinastraShift);
                        turnoShift.setStartTime(inicioSemana);
                        turnoShift.setEndTime(inicioSemana.plusDays(4)); // Sexta-feira
                        turnoShift.setTurnValue(BigDecimal.ZERO);
                        turnoShift.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                        turnRepository.save(turnoShift);
                    }
                }

            } else {
                alertasGerados.add("⚠️ Semana de " + inicioSemana + ": Recursos insuficientes (Precisava de " + recursosNecessarios + " colaboradores novos, mas só há " + candidatos.size() + ").");
            }

            // Avança para a próxima semana
            semanaAtual = semanaAtual.plusDays(7);
        }

        // Registo da ação de auditoria no final do processo
        auditLogService.log(adminActor, "GENERATE_SCHEDULE", "System", null,
                String.format("Automatic generation triggered for period %s to %s", dataInicio, dataFim));

        return alertasGerados;
    }

    private boolean checkFechoDeMes(LocalDate inicio, LocalDate fim) {
        return inicio.getMonth() != fim.getMonth() || fim.getDayOfMonth() >= 28;
    }
}