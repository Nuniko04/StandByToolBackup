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
import java.time.OffsetDateTime;
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
                //standBy.setGoogleCalendarId("c_127782d783f73cf69d78f5b5c3267572c76c15837d861100aefe661bff5e0798@group.calendar.google.com");
                typeRepo.save(standBy);

                TurnType backup = new TurnType();
                backup.setName("Backup");
                backup.setDefaultValue(new BigDecimal("30.00"));
                backup.setGoogleCalendarId("c_7cf870a9228bcccfb004bec53419bf49c263498b3bc1e8f57f8036efa762de3d@group.calendar.google.com");
                //backup.setGoogleCalendarId("c_127782d783f73cf69d78f5b5c3267572c76c15837d861100aefe661bff5e0798@group.calendar.google.com");
                typeRepo.save(backup);

                TurnType finastra = new TurnType();
                finastra.setName("Finastra Shift");
                finastra.setDefaultValue(new BigDecimal("0.00"));
                finastra.setGoogleCalendarId("c_dd2dd795931a4ea7f07a5cde6c3c98ac971cba85a14cfc5cea985136e0a13729@group.calendar.google.com");
                //finastra.setGoogleCalendarId("c_839934d66b831c2fb95d1e512ab5bbec517e8b6daa1579989f1dd9dc252fd8f5@group.calendar.google.com");
                typeRepo.save(finastra);

                System.out.println("🔧 Tipos de Turno criados com sucesso!");
            }

            // 2. Cria um Turno de Teste para o Nuno (se não houver turnos na BD)
            if (turnRepo.count() == 0) {
                List<User> users = userRepo.findAll();
                if (!users.isEmpty()) {
                    User oNuno = users.get(0); // Apanha o seu utilizador

                    Turn turnoTeste = new Turn();
                    turnoTeste.setAssignee(oNuno);
                    turnoTeste.setTurnType(typeRepo.findByName("Finastra Shift"));

                    // Começa amanhã, acaba daqui a 8 dias
                    turnoTeste.setStartTime(LocalDate.of(2026, 5, 18));
                    turnoTeste.setEndTime(LocalDate.of(2026, 5, 22));

                    turnoTeste.setTurnValue(new BigDecimal("50.00"));
                    turnoTeste.setTurnStatus(TurnStatus.PENDING_ACCEPTANCE);

                    Turn guardado = turnRepo.save(turnoTeste);

                    System.out.println("==================================================");
                    System.out.println("🎉 TURNO DE TESTE CRIADO! COPIE ESTE ID:");
                    System.out.println(guardado.getId());
                    System.out.println("==================================================");
                }
            }
        };
    }
}