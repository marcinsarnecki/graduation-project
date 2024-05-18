package uwr.ms.controller;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {
    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String error(Model model, WebRequest webRequest) {
        ErrorAttributeOptions options = ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.BINDING_ERRORS,
                ErrorAttributeOptions.Include.STACK_TRACE,
                ErrorAttributeOptions.Include.EXCEPTION
        );

        Map<String, Object> errors = this.errorAttributes.getErrorAttributes(webRequest, options);
        model.addAttribute("error", errors.get("error"));
        model.addAttribute("message", errors.get("message"));
        model.addAttribute("status", errors.get("status"));
        model.addAttribute("timestamp", errors.get("timestamp"));
        return "app-user/error";
    }
}