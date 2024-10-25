package uwr.ms.controller;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import uwr.ms.constant.LoginProvider;
import uwr.ms.constant.Message;
import uwr.ms.exception.ValidationException;
import uwr.ms.model.AppUser;
import jakarta.validation.Valid;
import lombok.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.ms.service.AppUserService;

import java.util.List;

import static uwr.ms.constant.Authority.STANDARD_USER;

@Controller
@RequestMapping("/app-user")
@Value
public class AppUserController {
    AppUserService appUserService;

    public record SignUpRequest(String username, String email, String password) {}

    @GetMapping("/signup")
    public String getSignUp() {
        return "app-user/signup";
    }

    @PostMapping("/signup")
    public String signUp(@Valid SignUpRequest signUpRequest, RedirectAttributes redirectAttributes) {
        try {
            appUserService.createUser(AppUser.builder()
                    .username(signUpRequest.username())
                    .email(signUpRequest.email())
                    .password(signUpRequest.password())
                    .provider(LoginProvider.APP)
                    .authorities(List.of(new SimpleGrantedAuthority(STANDARD_USER.name())))
                    .build());
            redirectAttributes.addFlashAttribute("successMessage", Message.REGISTRATION_SUCCESS.toString());
            return "redirect:/login";
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getErrors());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getMessage());
        }
        return "redirect:/app-user/signup";
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmNewPassword) {}

    @GetMapping("/change-password")
    public String getChangePassword() {
        return "app-user/change_password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid ChangePasswordRequest changePasswordRequest, RedirectAttributes redirectAttributes) {
        try {
            appUserService.validateAndChangePassword(changePasswordRequest);
            redirectAttributes.addFlashAttribute("successMessage", Message.PASSWORD_CHANGE_SUCCESS.toString());
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getErrors());
            return "redirect:/app-user/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.PASSWORD_CHANGE_FAILED.toString(), e.getMessage()));
            return "redirect:/app-user/change-password";
        }
        return "redirect:/app-user/change-password";
    }

    public record EditProfileRequest(String imageUrl, String name, String email) {}

    @GetMapping("/edit-profile")
    public String getEditProfile(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = (AppUser) appUserService.loadUserByUsername(username);
        model.addAttribute("user", user);
        return "app-user/edit_profile";
    }

    @PostMapping("/edit-profile")
    public String editProfile(@ModelAttribute EditProfileRequest editProfileRequest, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            appUserService.updateUserProfile(username, editProfileRequest);
            redirectAttributes.addFlashAttribute("successMessage", Message.PROFILE_UPDATE_SUCCESS.toString());
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessages", e.getErrors());
        }
        catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.PROFILE_UPDATE_FAILED.toString(), e.getMessage()));
        }
        return "redirect:/app-user/edit-profile";
    }

    @GetMapping("/confirm-delete-account")
    public String confirmDeleteAccount() {
        return "app-user/confirm_delete_account";
    }
    @PostMapping("/delete-account")
    public String deleteAccount(@RequestParam("password") String password, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            appUserService.verifyPassword(username, password);
            appUserService.deleteUser(username);
            redirectAttributes.addFlashAttribute("successMessage", Message.ACCOUNT_DELETION_SUCCESS.toString());
            return "redirect:/login";
        }
        catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessages", String.format(Message.ACCOUNT_DELETION_FAILED.toString(), e.getMessage()));
            return "redirect:/app-user/edit-profile";
        }
    }
}

