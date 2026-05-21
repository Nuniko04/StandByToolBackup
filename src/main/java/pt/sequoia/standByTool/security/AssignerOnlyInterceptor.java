package pt.sequoia.standByTool.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.util.Optional;

@Component
public class AssignerOnlyInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    // Injetamos o repositório para podermos ir à Base de Dados
    public AssignerOnlyInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User sessionUser = (User) session.getAttribute("loggedUser");
            if (sessionUser != null) {
                // Em vez de confiarmos cegamente na sessão, vamos verificar à Base de Dados!
                Optional<User> dbUser = userRepository.findById(sessionUser.getId());

                if (dbUser.isPresent() && dbUser.get().isAssigner()) {
                    // A pessoa continua a ser Assigner na BD. Pode passar!

                    // Bónus: Atualizamos a sessão silenciosamente caso o nome/email tenha mudado
                    session.setAttribute("loggedUser", dbUser.get());
                    return true;
                }
            }
        }

        // Se chegou aqui, ou não tem sessão, ou já não é Assigner na BD. Rua!
        response.sendRedirect("/employee-view");
        return false;
    }
}