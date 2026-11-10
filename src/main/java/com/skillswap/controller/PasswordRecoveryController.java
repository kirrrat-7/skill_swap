package com.skillswap.controller;

import com.skillswap.dto.ForgotPasswordForm;
import com.skillswap.dto.PasswordResetForm;
import com.skillswap.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PasswordRecoveryController {
    private final PasswordResetService passwordReset;

    public PasswordRecoveryController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    @GetMapping("/forgot-password")
    public String forgotPage(Model model, Authentication authentication) {
        if (isSignedIn(authentication)) return "redirect:/dashboard";
        if (!model.containsAttribute("form")) model.addAttribute("form", new ForgotPasswordForm());
        return "account/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("form") ForgotPasswordForm form,
                               BindingResult errors, HttpServletRequest request) {
        if (errors.hasErrors()) return "account/forgot-password";
        try {
            passwordReset.requestReset(form.getEmail(), request.getRemoteAddr());
        } catch (org.springframework.mail.MailException | IllegalStateException ignored) {
            // Keep the response identical whether the address exists, is limited, or mail is unavailable.
        }
        return "redirect:/forgot-password/sent";
    }

    @GetMapping("/forgot-password/sent")
    public String sent() { return "account/forgot-password-sent"; }

    @GetMapping("/reset-password")
    public String resetPage(@RequestParam(required = false) String token, Model model) {
        boolean usable = passwordReset.isUsable(token);
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("tokenUsable", usable);
        if (!model.containsAttribute("form")) model.addAttribute("form", new PasswordResetForm());
        return "account/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @Valid @ModelAttribute("form") PasswordResetForm form,
                        BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!java.util.Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("token", token);
            model.addAttribute("tokenUsable", passwordReset.isUsable(token));
            return "account/reset-password";
        }
        if (!passwordReset.reset(token, form.getPassword())) {
            model.addAttribute("token", "");
            model.addAttribute("tokenUsable", false);
            return "account/reset-password";
        }
        redirect.addFlashAttribute("passwordReset", true);
        return "redirect:/login?reset";
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
