package pt.sequoia.standByTool.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Se for o próprio sistema (ex: CommandLineRunner ao iniciar)
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.of("Sistema");
        }

        // Se for um utilizador real logado pelo Google (OIDC)
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oauth2User = (OidcUser) authentication.getPrincipal();
            return Optional.of(oauth2User.getEmail()); // Regista o email de quem fez a ação
        }

        return Optional.of(authentication.getName());
    }
}