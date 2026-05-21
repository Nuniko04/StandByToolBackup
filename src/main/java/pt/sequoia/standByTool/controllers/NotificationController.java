package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.services.NotificationService;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead(HttpServletRequest request, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser != null) {
            notificationService.marcarTodasComoLidas(loggedUser.getId());
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}