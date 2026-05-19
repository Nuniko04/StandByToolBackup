package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Traz todas as notificações de um utilizador, ordenadas da mais recente para a mais antiga
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Conta quantas notificações não lidas a pessoa tem (para pôr um balãozinho vermelho com o número)
    long countByUserIdAndIsReadFalse(UUID userId);
}