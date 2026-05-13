package pt.sequoia.standByTool.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // <-- ERA ISTO QUE FALTAVA! Sem isto o Spring ignora o ficheiro.
public class IndexController {

    @GetMapping("/")
    public String index() {
        // Redireciona quem entra na raiz para o dashboard
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        // Mostra o dashboard.html
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        // Mostra o login.html
        return "login";
    }
}