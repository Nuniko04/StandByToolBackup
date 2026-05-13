package pt.sequoia.standByTool.services;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.repositories.UserRepository;

@Service
public class UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final HttpSession httpSession;

    public UserService(UserRepository userRepository, HttpSession httpSession) {
        this.userRepository = userRepository;
        this.httpSession = httpSession;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // Usa o motor OIDC correto da Google
        OidcUser oidcUser = super.loadUser(userRequest);

        // A Google usa o 'Subject' para enviar o ID
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getAttribute("name");

        // Procura na BD. Se não encontrar, cria um novo (orElseGet)
        User user = userRepository.findByGoogleOauthId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleOauthId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAssigner(true);
            newUser.setEmployee(false);

            System.out.println("🌟 NOVO UTILIZADOR CRIADO NA BD: " + email); // <-- Log só para novos!
            return userRepository.save(newUser);
        });

        // Guarda a nossa entidade User na sessão para o Interceptor ver
        httpSession.setAttribute("loggedUser", user);

        // Imprime na consola que a pessoa fez login com sucesso
        System.out.println("✅ UTILIZADOR AUTENTICADO: " + email);

        return oidcUser;
    }
}