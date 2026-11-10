package com.skillswap.controller;

import com.skillswap.dto.RegistrationForm;
import com.skillswap.service.EmailAlreadyInUseException;
import com.skillswap.service.EmailVerificationResult;
import com.skillswap.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
    private final RegistrationService registration;

    public AccountController(RegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping("/register")
    public String registerPage(Model model, Authentication authentication) {
        if (isSignedIn(authentication)) return "redirect:/dashboard";
        if (!model.containsAttribute("form")) model.addAttribute("form", new RegistrationForm());
        return "account/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult errors, RedirectAttributes redirect) {
        if (!java.util.Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (errors.hasErrors()) return "account/register";
        try {
            registration.register(form);
        } catch (EmailAlreadyInUseException ex) {
            errors.rejectValue("email", "email.duplicate", ex.getMessage());
            return "account/register";
        } catch (IllegalStateException | org.springframework.mail.MailException ex) {
            errors.reject("mail.unavailable", "We could not send the verification email. Please try again later.");
            return "account/register";
        }
        redirect.addFlashAttribute("registeredEmail", form.getEmail().strip());
        return "redirect:/verify-email/sent";
    }

    @GetMapping("/verify-email/sent")
    public String verificationSent(Model model, @org.springframework.beans.factory.annotation.Value("${skillswap.mail.dev-outbox:false}") boolean devOutboxEnabled) {
        model.addAttribute("devOutboxEnabled", devOutboxEnabled);
        return "account/verification-sent";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) {
        EmailVerificationResult result = registration.verify(token);
        model.addAttribute("verificationResult", result);
        return "account/verification-result";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isSignedIn(authentication)) return "redirect:/dashboard";
        return "account/login";
    }

    private static boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
