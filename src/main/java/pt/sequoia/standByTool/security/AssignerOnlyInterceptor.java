package pt.sequoia.standByTool.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.sequoia.standByTool.models.User;

@Component
public class AssignerOnlyInterceptor implements HandlerInterceptor {

    // Vai ler o valor do nosso application.properties
    @Value("${app.launch.assigner-only-mode:false}")
    private boolean isAssignerOnlyMode;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Se o interruptor estiver desligado, toda a gente passa!
        if (!isAssignerOnlyMode) {
            return true;
        }

        // Se estiver ligado, temos de ver quem é o utilizador.
        // (Nota: A forma de ir buscar o utilizador depende de como estão a fazer o Login.
        // Assumindo que o guardaram na Sessão ao fazer login com o Google OAuth):
        User utilizadorLogado = (User) request.getSession().getAttribute("loggedUser");

        // Se o utilizador existir, mas a flag "isAssigner" for falsa...
        if (utilizadorLogado != null && !utilizadorLogado.isAssigner()) {

            // ... redirecionamos para uma página bonita a dizer "Aplicação em fase de testes"
            response.sendRedirect("/coming-soon");
            return false; // Bloqueia o acesso ao dashboard normal
        }

        // Se for o Assigner, deixa passar
        return true;
    }
}