package uwr.ms;

import uwr.ms.security.AppUser;
import uwr.ms.security.AppUserService;
import uwr.ms.security.LoginProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/app-user")
@Log4j2
@Value
public class AppUserController {
    AppUserService appUserService;
    public record SignUpRequest(
            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Email should be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}
    @GetMapping("/signup")
    public String getSignUp() {
        return "app-user/signup";
    }

    @PostMapping("/signup")
    public String postSignUp(@Valid SignUpRequest signUpRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        List<String> errors = new ArrayList<>();
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> errors.add(error.getDefaultMessage()));
        }
        errors.addAll(validatePassword(signUpRequest.password()));
        // TODO check if passwd is ok !!!
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessages", errors);
            return "redirect:/app-user/signup";
        }
        try {
            appUserService.createUser(AppUser.builder()
                    .username(signUpRequest.username())
                    .email(signUpRequest.email())
                    .password(signUpRequest.password())
                    .provider(LoginProvider.APP)
//                    .attributes() // TODO basic authorities
//                    .authorities()
                    .build());
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful, you can now log in");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
            return "redirect:/app-user/signup";
        }
    }

    @GetMapping("/change-password")
    public String getChangePassword() {
        return "app-user/change_password";
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            String newPassword,

            @NotBlank(message = "Confirm new password is required")
            String confirmNewPassword
    ) {}
    private List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }
        return errors;
    }

    @PostMapping("/change-password")
    public String postChangePassword(@Valid AppUserController.ChangePasswordRequest changePasswordRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        List<String> errors = new ArrayList<>();
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> errors.add(error.getDefaultMessage()));
        }
        errors.addAll(validatePassword(changePasswordRequest.newPassword()));
        if (!changePasswordRequest.newPassword().equals(changePasswordRequest.confirmNewPassword())) {
            errors.add("Confirmed new password does not match with new password");
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessages", errors);
            return "redirect:/app-user/change-password";
        }
        appUserService.changePassword(changePasswordRequest.currentPassword, changePasswordRequest.newPassword());
        redirectAttributes.addFlashAttribute("successMessage", "Password successfully changed");
        return "redirect:/app-user/change-password";
    }
}
