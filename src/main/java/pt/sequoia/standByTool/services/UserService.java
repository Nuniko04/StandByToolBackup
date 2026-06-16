package pt.sequoia.standByTool.services;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import pt.sequoia.standByTool.models.TurnType;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.UserStatus;
import pt.sequoia.standByTool.repositories.TurnTypeRepository;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final TurnTypeRepository turnTypeRepository;
    private final HttpSession httpSession;

    public UserService(UserRepository userRepository, TurnTypeRepository turnTypeRepository, HttpSession httpSession) {
        this.userRepository = userRepository;
        this.turnTypeRepository = turnTypeRepository;
        this.httpSession = httpSession;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getAttribute("name");

        User user = userRepository.findByGoogleOauthId(googleId).orElseGet(() -> {
            return userRepository.findByEmail(email).map(existingUser -> {
                existingUser.setGoogleOauthId(googleId);
                return userRepository.save(existingUser);
            }).orElseGet(() -> {
                User newUser = new User();
                newUser.setGoogleOauthId(googleId);
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setAssigner(false);
                newUser.setStatus(UserStatus.ACTIVE);
                return userRepository.save(newUser);
            });
        });

        httpSession.setAttribute("loggedUser", user);
        return oidcUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void createUser(String name, String email, boolean isAssigner, User adminActor) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setAssigner(isAssigner);
        newUser.setStatus(UserStatus.ACTIVE);

        userRepository.save(newUser);
    }

    // --- NOVO: MÉTODO PARA EDITAR OS DADOS DO COLABORADOR ---
    @Transactional
    public void updateUserDetails(UUID id, String name, String email, boolean isAssigner, User adminActor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado."));

        user.setName(name);
        user.setEmail(email);
        user.setAssigner(isAssigner);

        userRepository.save(user);
    }

    @Transactional
    public boolean toggleUserStatus(UUID id, User adminActor) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User user = opt.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                user.setStatus(UserStatus.INACTIVE);
            } else {
                user.setStatus(UserStatus.ACTIVE);
            }
            userRepository.save(user);

            return true;
        }
        return false;
    }

    @Transactional
    public boolean toggleUserRole(UUID id, User adminActor) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User user = opt.get();
            user.setAssigner(!user.isAssigner());
            userRepository.save(user);

            return true;
        }
        return false;
    }

    // =========================================================================
    // MÉTODOS DA MATRIZ DE ELEGIBILIDADE
    // =========================================================================

    @Transactional
    public boolean updateEligibility(UUID userId, List<UUID> turnTypeIds, User adminActor) {
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()) {
            User user = optUser.get();

            // Vai buscar à BD as instâncias verdadeiras dos turnos através dos IDs enviados pelo formulário
            List<TurnType> turnTypes = turnTypeRepository.findAllById(turnTypeIds);

            user.setEligibleTurnTypes(turnTypes);
            userRepository.save(user);

            return true;
        }
        return false;
    }

    // Mapa para uso seguro no HTML, evitando o erro de LazyInitializationException no Thymeleaf
    @Transactional(readOnly = true)
    public java.util.Map<UUID, java.util.List<UUID>> getUserEligibilitiesMap() {
        java.util.Map<UUID, java.util.List<UUID>> map = new java.util.HashMap<>();
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (user.getEligibleTurnTypes() != null) {
                java.util.List<UUID> allowedIds = user.getEligibleTurnTypes().stream()
                        .map(pt.sequoia.standByTool.models.TurnType::getId)
                        .toList();
                map.put(user.getId(), allowedIds);
            } else {
                map.put(user.getId(), new java.util.ArrayList<>());
            }
        }
        return map;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}