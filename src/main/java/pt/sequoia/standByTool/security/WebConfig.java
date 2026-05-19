package pt.sequoia.standByTool.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AssignerOnlyInterceptor assignerOnlyInterceptor;

    public WebConfig(AssignerOnlyInterceptor assignerOnlyInterceptor) {
        this.assignerOnlyInterceptor = assignerOnlyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica o bloqueio a tudo, exceto ao login e aos ficheiros de design
        registry.addInterceptor(assignerOnlyInterceptor)
                .addPathPatterns("/dashboard", "/dashboardAssigner", "/api/**")
                .excludePathPatterns("/login", "/coming-soon", "/css/**", "/js/**", "/error");
    }
}