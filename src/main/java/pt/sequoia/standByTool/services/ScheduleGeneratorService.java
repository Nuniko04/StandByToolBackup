package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.TemporalAdjusters;

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
    public List<String> gerarEscalas(LocalDate dataInicio, LocalDate dataFim, User adminActor) {

        List<String> alertasGerados = new ArrayList<>();

        // 🔒 GARANTIA: Se o Assigner escolher uma 4ª feira, o sistema ajusta automaticamente para a 2ª feira dessa semana!
        LocalDate semanaAtual = dataInicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

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

            // Substitui estas 3 linhas:
            boolean faltaStandBy = !turnRepository.existsTurnOfTypeInWeek("StandBy", inicioSemana.atStartOfDay());
            boolean faltaBackup = !turnRepository.existsTurnOfTypeInWeek("Backup", inicioSemana.atStartOfDay());
            boolean faltaFinastra = isHoraDeVerao && !turnRepository.existsTurnOfTypeInWeek("Finastra Shift", inicioSemana.atStartOfDay());

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

            // ---------------------------------------------------------
            // 2. FASE DE EXCLUSÃO (A Inteligência das Férias)
            // ---------------------------------------------------------
            for (User user : todosColaboradores) {

                // A) Está de férias durante a semana atual? (Bloqueia sempre)
                boolean feriasNestaSemana = requestRepository.hasApprovedVacationOverlapping(user.getId(), inicioSemana, fimSemana);

                // B) A regra de Ouro: As férias começam na SEGUNDA-FEIRA da próxima semana?
                // O 'proxSemanaInicio' já é garantidamente uma Segunda-feira através do teu 'TemporalAdjusters'
                boolean feriasComecamProximaSegunda = requestRepository.hasApprovedVacationStartingOn(user.getId(), proxSemanaInicio);

                // C) Já tem um turno atribuído nestas datas?
                boolean temTurno = turnRepository.existsByAssigneeAndDates(user.getId(), inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));

                // Se não esbarrar em nenhuma das restrições, é candidato!
                if (!feriasNestaSemana && !feriasComecamProximaSegunda && !temTurno) {
                    candidatos.add(new ColaboradorScore(user));
                }
            }

            // 3. FASE DE SCORING (Equidade e Cadência)
            boolean isSemanaComFeriado = feriadoRepository.existsFeriadoInPeriod(inicioSemana, fimSemana);
            boolean isSemanaFechoMes = checkFechoDeMes(inicioSemana, fimSemana);

            for (ColaboradorScore candidato : candidatos) {
                // Substitui a linha de verificar semanas desde o último turno:
                Integer semanasDb = turnRepository.getWeeksSinceLastTurn(candidato.user.getId(), inicioSemana.atStartOfDay());
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
                    turnoStandBy.setStartTime(inicioSemana.atStartOfDay()); // Segunda-feira às 00:00
                    turnoStandBy.setEndTime(inicioSemana.plusDays(6).atTime(23, 59, 59)); // Domingo às 23:59:59
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
                    turnoBackup.setStartTime(inicioSemana.atStartOfDay()); // Segunda-feira às 00:00
                    turnoBackup.setEndTime(inicioSemana.plusDays(6).atTime(23, 59, 59)); // Domingo às 23:59:59
                    turnoBackup.setTurnValue(BigDecimal.ZERO);
                    turnoBackup.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);
                    turnRepository.save(turnoBackup);

                    indiceAtual++;
                }

                // ---------------------------------------------------------
                // PREENCHIMENTO DO BURACO: FINASTRA SHIFT
                // ---------------------------------------------------------
                if (faltaFinastra) {
                    User melhorParaShift = null;

                    // Procura o melhor APENAS entre os candidatos que sobraram E que têm a permissão na Matriz
                    for (int i = indiceAtual; i < candidatos.size(); i++) {
                        User candidatoAtual = candidatos.get(i).user;

                        // Verifica na nossa tabela de elegibilidade se ele tem o tipo "Finastra"
                        boolean isEligible = candidatoAtual.getEligibleTurnTypes().stream()
                                .anyMatch(tt -> tt.getId().equals(tipoFinastraShift.getId()));

                        if (isEligible) {
                            melhorParaShift = candidatoAtual;
                            break; // Encontrámos o nosso homem/mulher!
                        }
                    }

                    if (melhorParaShift == null) {
                        alertasGerados.add("⚠️ Semana de " + inicioSemana + ": Ninguém com permissão 'Finastra Shift' disponível para preencher o turno em falta!");
                    } else {
                        Turn turnoShift = new Turn();
                        turnoShift.setAssignee(melhorParaShift);
                        turnoShift.setTurnType(tipoFinastraShift);
                        turnoShift.setStartTime(inicioSemana.atStartOfDay());
                        turnoShift.setEndTime(inicioSemana.plusDays(4).atTime(23, 59, 59)); // Sexta-feira às 23:59
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

        // Registo da ação de auditoria no final do processo usando o ID do Assigner
        auditLogService.log(adminActor, "GENERATE_SCHEDULE", "System", adminActor.getId(),
                String.format("Automatic generation triggered for period %s to %s", dataInicio, dataFim));

        return alertasGerados;
    }

    private boolean checkFechoDeMes(LocalDate inicio, LocalDate fim) {
        return inicio.getMonth() != fim.getMonth() || fim.getDayOfMonth() >= 28;
    }
}