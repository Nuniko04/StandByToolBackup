package pt.sequoia.standByTool.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import pt.sequoia.standByTool.services.UserService;

import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    // Injetamos o nosso serviço de utilizadores aqui
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority oidcAuth) {
                    String email = oidcAuth.getIdToken().getEmail();

                    userService.findByEmail(email).ifPresent(user -> {
                        if (user.isAssigner()) {
                            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        } else {
                            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
                        }
                    });
                }
                mappedAuthorities.add(authority);
            });

            return mappedAuthorities;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/error", "/coming-soon", "/api/finance/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // STRICT RBAC: Rotas exclusivas para os Assigners
                        .requestMatchers(
                                "/turntypes/**",
                                "/users/**",
                                "/cards/**",
                                "/feriados/**",
                                "/api/servicos-cliente/**",
                                "/turns/assign",
                                "/turns/*/delete",
                                "/requests/*/approve",
                                "/requests/*/reject"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(userService)
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                        .defaultSuccessUrl("/dashboard", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}