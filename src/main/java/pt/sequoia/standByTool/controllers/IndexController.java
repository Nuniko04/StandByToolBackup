package pt.sequoia.standByTool.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pt.sequoia.standByTool.models.Turn;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.TurnStatus;
import pt.sequoia.standByTool.services.RequestService;
import pt.sequoia.standByTool.services.TurnService;
import pt.sequoia.standByTool.services.TurnTypeService;
import pt.sequoia.standByTool.services.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class IndexController {

    private final TurnService turnService;
    private final RequestService requestService;
    private final UserService userService;
    private final TurnTypeService turnTypeService;

    public IndexController(TurnService turnService, RequestService requestService, UserService userService, TurnTypeService turnTypeService) {
        this.turnService = turnService;
        this.requestService = requestService;
        this.userService = userService;
        this.turnTypeService = turnTypeService;
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

            model.addAttribute("pendingAcceptanceCount", allTurns.stream().filter(t -> t.getTurnStatus() == TurnStatus.PENDING_ACCEPTANCE).count());
            model.addAttribute("confirmedCount", allTurns.stream().filter(t -> t.getTurnStatus() == TurnStatus.ACCEPTED || t.getTurnStatus() == TurnStatus.COMPLETED).count());

            // NOVO: Passar a lista completa de pedidos para o Modal do Assigner
            model.addAttribute("pendingRequests", requestService.getPendingRequests());
            model.addAttribute("pendingRequestsCount", requestService.getPendingRequests().size());

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

        // NOVO: Passar a lista de colegas (excluindo o próprio utilizador logado)
        List<User> colleagues = userService.getAllUsers().stream()
                .filter(u -> !u.getId().equals(loggedUser.getId()))
                .collect(Collectors.toList());

        model.addAttribute("user", loggedUser);
        model.addAttribute("myTurns", myTurns);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("colleagues", colleagues); // Injeta os colegas para o Select do Modal de Swap

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