package pt.sequoia.standByTool.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import pt.sequoia.standByTool.services.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    // Injetamos o nosso serviço de utilizadores aqui
    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 💡 ADICIONADO O CAMINHO /api/finance/** PARA O SCHEDULER
                        .requestMatchers("/", "/login", "/error", "/coming-soon", "/api/finance/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        // MUDE AQUI DE .userService PARA .oidcUserService
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(userService))
                        .defaultSuccessUrl("/dashboard", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // A rota invisível que o Spring escuta
                        .logoutSuccessUrl("/login?logout=true") // Redireciona para o login com um aviso
                        .invalidateHttpSession(true) // Destrói a sessão (e o nosso 'loggedUser' vai ao ar!)
                        .clearAuthentication(true) // Limpa o contexto de segurança
                        .deleteCookies("JSESSIONID") // Apaga o cookie do browser
                        .permitAll()
                );

        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}