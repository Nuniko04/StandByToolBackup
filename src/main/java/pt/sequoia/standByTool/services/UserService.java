package pt.sequoia.standByTool.services;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sequoia.standByTool.models.User;
import pt.sequoia.standByTool.models.enums.UserStatus;
import pt.sequoia.standByTool.repositories.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final HttpSession httpSession;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, HttpSession httpSession, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.httpSession = httpSession;
        this.auditLogService = auditLogService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getAttribute("name");

        User user = userRepository.findByGoogleOauthId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleOauthId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAssigner(true);
            newUser.setEmployee(false);
            return userRepository.save(newUser);
        });

        httpSession.setAttribute("loggedUser", user);
        return oidcUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public boolean toggleUserStatus(UUID userId, User adminActor) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserStatus oldStatus = user.getStatus();
            user.setStatus(oldStatus == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
            userRepository.save(user);

            auditLogService.log(adminActor, "TOGGLE_USER_STATUS", "User", userId,
                    "Status changed: " + oldStatus + " -> " + user.getStatus());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean updateUserPermissions(UUID userId, boolean isAssigner, boolean isFinastraEligible, User adminActor) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String details = String.format("Permissions updated - isAssigner: %b -> %b, isFinastra: %b -> %b",
                    user.isAssigner(), isAssigner, user.isFinastraEligible(), isFinastraEligible);

            user.setAssigner(isAssigner);
            user.setFinastraEligible(isFinastraEligible);
            userRepository.save(user);

            auditLogService.log(adminActor, "UPDATE_USER_PERMISSIONS", "User", userId, details);
            return true;
        }
        return false;
    }
}