package pt.sequoia.standByTool;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.repositories.TurnRepository;
import pt.sequoia.standByTool.repositories.TurnTypeRepository;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    // Este código corre automaticamente sempre que liga a aplicação!
    @Bean
    CommandLineRunner initData(TurnTypeRepository typeRepo, TurnRepository turnRepo, UserRepository userRepo) {
        return args -> {
            // 1. Cria os Tipos de Turno base (se a tabela estiver vazia)
            if (typeRepo.count() == 0) {
                TurnType standBy = new TurnType();
                standBy.setName("StandBy");
                standBy.setDefaultValue(new BigDecimal("50.00"));
                standBy.setGoogleCalendarId("c_7cf870a9228bcccfb004bec53419bf49c263498b3bc1e8f57f8036efa762de3d@group.calendar.google.com");
                standBy.setEligibleForAutoGeneration(true);
                standBy.setDeleted(false);
                typeRepo.save(standBy);

                TurnType backup = new TurnType();
                backup.setName("Backup");
                backup.setDefaultValue(new BigDecimal("30.00"));
                backup.setGoogleCalendarId("c_7cf870a9228bcccfb004bec53419bf49c263498b3bc1e8f57f8036efa762de3d@group.calendar.google.com");
                backup.setEligibleForAutoGeneration(true);
                backup.setDeleted(false);
                typeRepo.save(backup);

                TurnType finastra = new TurnType();
                finastra.setName("Finastra Shift");
                finastra.setDefaultValue(new BigDecimal("0.00"));
                finastra.setGoogleCalendarId("c_dd2dd795931a4ea7f07a5cde6c3c98ac971cba85a14cfc5cea985136e0a13729@group.calendar.google.com");
                finastra.setEligibleForAutoGeneration(true);
                finastra.setDeleted(false);
                typeRepo.save(finastra);

                System.out.println("🔧 Tipos de Turno criados com sucesso!");
            }

            // 2. Cria ou Identifica o Turno de Teste
            long turnCount = turnRepo.count();
            List<User> users = userRepo.findAll();
            TurnType finastraType = typeRepo.findByName("Finastra Shift");

            if (turnCount == 0) {
                if (!users.isEmpty() && finastraType != null) {
                    User oNuno = users.get(0);

                    Turn turnoTeste = new Turn();
                    turnoTeste.setAssignee(oNuno);
                    turnoTeste.setTurnType(finastraType);
                    turnoTeste.setStartTime(LocalDateTime.of(2026, 5, 18, 0, 0));
                    turnoTeste.setEndTime(LocalDateTime.of(2026, 5, 22, 23, 59));
                    turnoTeste.setTurnValue(new BigDecimal("50.00"));
                    turnoTeste.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);

                    Turn guardado = turnRepo.save(turnoTeste);

                    System.out.println("==================================================");
                    System.out.println("🎉 TURNO DE TESTE CRIADO! COPIE ESTE ID:");
                    System.out.println(guardado.getId());
                    System.out.println("==================================================");
                } else {
                    System.err.println("==================================================");
                    System.err.println("❌ ERRO NA CRIAÇÃO DO TURNO DE TESTE:");
                    if (users.isEmpty()) System.err.println("👉 Motivo: Tabela de utilizadores vazia. Faça login no browser.");
                    if (finastraType == null) System.err.println("👉 Motivo: Tipo de turno 'Finastra Shift' não encontrado.");
                    System.err.println("==================================================");
                }
            } else {
                // SE JÁ HOUVER TURNOS, ELE MOSTRA O PRIMEIRO QUE ENCONTRAR
                Turn existente = turnRepo.findAll().get(0);
                System.out.println("==================================================");
                System.out.println("ℹ️ INFO: Já existem " + turnCount + " turnos na base de dados.");
                System.out.println("👉 ID do primeiro turno: " + existente.getId());
                System.out.println("👉 Status atual: " + existente.getTurnStatus());
                System.out.println("==================================================");
            }
        };
    }
}