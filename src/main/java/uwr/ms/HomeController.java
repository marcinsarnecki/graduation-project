package uwr.ms;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "app-user/index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(name = "logout", required = false, defaultValue = "true") boolean logout) {
        return "app-user/login";
    }
}
