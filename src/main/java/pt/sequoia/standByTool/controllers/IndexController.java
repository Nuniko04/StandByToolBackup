package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pt.sequoia.standByTool.models.Notification;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.services.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    private final TurnService turnService;
    private final RequestService requestService;
    private final UserService userService;
    private final TurnTypeService turnTypeService;
    private final NotificationService notificationService;
    private final CardService cardService;

    public IndexController(TurnService turnService, RequestService requestService, UserService userService, TurnTypeService turnTypeService, NotificationService notificationService, CardService cardService) {
        this.turnService = turnService;
        this.requestService = requestService;
        this.userService = userService;
        this.turnTypeService = turnTypeService;
        this.notificationService = notificationService;
        this.cardService = cardService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        // ==========================================
        // ROTA 1: SE FOR UM ASSIGNER (RH / GESTOR)
        // ==========================================
        if (loggedUser.isAssigner()) {
            List<Turn> allTurns = turnService.getAllTurns();

            model.addAttribute("employees", userService.getAllUsers());
            model.addAttribute("turnTypes", turnTypeService.getAllTurnTypes());

            // INJETAR OS CARTÕES PARA APARECEREM NA ADMINISTRAÇÃO E ATRIBUIÇÃO DE TURNOS
            model.addAttribute("cards", cardService.getAllCards());

            model.addAttribute("pendingAcceptanceCount", allTurns.stream().filter(t -> t.getTurnStatus() == TurnStatus.PENDING_ACCEPTANCE).count());
            model.addAttribute("confirmedCount", allTurns.stream().filter(t -> t.getTurnStatus() == TurnStatus.ACCEPTED || t.getTurnStatus() == TurnStatus.COMPLETED).count());

            // PEDIDOS PENDENTES E HISTÓRICO (Para a nova Aba de Swap Requests)
            model.addAttribute("pendingRequests", requestService.getPendingRequests());
            model.addAttribute("pendingRequestsCount", requestService.getPendingRequests().size());

            // Filtra os pedidos que já não estão pendentes (Aprovados ou Rejeitados) para o Histórico
            model.addAttribute("historyRequests", requestService.getAllRequests().stream()
                    .filter(r -> r.getStatus() != RequestStatus.PENDING)
                    .collect(Collectors.toList()));

            model.addAttribute("user", loggedUser);
            model.addAttribute("allTurns", allTurns);

            return "dashboardAssigner";
        }

        // ==========================================
        // ROTA 2: SE FOR UM COLABORADOR NORMAL
        // ==========================================
        List<Turn> myTurns = turnService.getMyTurns(loggedUser.getId());

        long pendingCount = myTurns.stream()
                .filter(t -> t.getTurnStatus() == TurnStatus.PENDING_ACCEPTANCE)
                .count();

        List<User> colleagues = userService.getAllUsers().stream()
                .filter(u -> !u.getId().equals(loggedUser.getId()))
                .collect(Collectors.toList());

        model.addAttribute("user", loggedUser);
        model.addAttribute("myTurns", myTurns);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("colleagues", colleagues);

        List<Notification> notificacoes = notificationService.obterNotificacoesDoUtilizador(loggedUser.getId());
        long alertasNaoLidos = notificationService.contarNaoLidas(loggedUser.getId());

        model.addAttribute("notificacoes", notificacoes);
        model.addAttribute("alertasNaoLidos", alertasNaoLidos);

        return "dashboard";
    }

    @GetMapping("/employee-view")
    public String employeeView(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        List<Turn> myTurns = turnService.getMyTurns(user.getId());

        long pendingCount = myTurns.stream()
                .filter(t -> t.getTurnStatus() == TurnStatus.PENDING_ACCEPTANCE)
                .count();

        // ERRO CORRIGIDO NESTA LISTA:
        List<User> colleagues = userService.getAllUsers().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("myTurns", myTurns);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("colleagues", colleagues);

        List<Notification> notificacoes = notificationService.obterNotificacoesDoUtilizador(user.getId());
        long alertasNaoLidos = notificationService.contarNaoLidas(user.getId());

        model.addAttribute("notificacoes", notificacoes);
        model.addAttribute("alertasNaoLidos", alertasNaoLidos);

        model.addAttribute("isViewingAsEmployee", true);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/coming-soon")
    public String comingSoon() {
        return "coming-soon";
    }
}