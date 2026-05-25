package pt.sequoia.standByTool.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.Notification;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.NotificationRepository;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 1. Criar uma nova notificação (Usado pelo RequestService)
    @Transactional
    public void criarNotificacao(User user, String mensagem) {
        Notification notif = new Notification(user, mensagem);
        notificationRepository.save(notif);
    }

    // 2. Buscar notificações do utilizador (Usado pelo IndexController)
    public List<Notification> obterNotificacoesDoUtilizador(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 3. Contar as não lidas (Usado pelo IndexController)
    public long contarNaoLidas(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // (Bónus futuro) 4. Marcar uma notificação como lida
    @Transactional
    public void marcarComoLida(UUID notificacaoId) {
        notificationRepository.findById(notificacaoId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void marcarTodasComoLidas(UUID userId) {
        List<Notification> notificacoes = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        boolean algumaAlterada = false;

        for (Notification n : notificacoes) {
            if (!n.isRead()) {
                n.setRead(true);
                algumaAlterada = true;
            }
        }

        if (algumaAlterada) {
            notificationRepository.saveAll(notificacoes);
        }
    }
}