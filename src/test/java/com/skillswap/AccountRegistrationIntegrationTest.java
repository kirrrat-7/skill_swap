package com.skillswap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillswap.service.DevMailOutboxService;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRegistrationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DevMailOutboxService outbox;

    @BeforeEach
    void clearOutbox() { outbox.clear(); }

    @AfterEach
    void removeTestAccounts() {
        jdbc.update("DELETE FROM users WHERE email IN (?, ?, ?, ?)",
            "asha.registration@example.edu", "ravi.verification@example.edu",
            "mina.invalid@example.edu", "duplicate.registration@example.edu");
        outbox.clear();
    }

    @Test
    void registrationCreatesAnUnverifiedAccountAndSendsAHashedTokenLink() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Asha Mehta")
                .param("email", "asha.registration@example.edu")
                .param("password", "BetterLearning2026")
                .param("confirmPassword", "BetterLearning2026")
                .param("college", "North Campus University")
                .param("department", "Computer Science")
                .param("semester", "4")
                .param("termsAccepted", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/verify-email/sent"));

        var row = jdbc.queryForMap("SELECT password_hash, email_verified_at FROM users WHERE email = ?", "asha.registration@example.edu");
        String hash = (String) row.get("password_hash");
        org.junit.jupiter.api.Assertions.assertTrue(hash.startsWith("$2"), "stored password must be BCrypt-hashed");
        org.junit.jupiter.api.Assertions.assertNotEquals("BetterLearning2026", hash);
        org.junit.jupiter.api.Assertions.assertNull(row.get("email_verified_at"));
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.latest().size());

        String email = outbox.latest().getFirst().text();
        Matcher linkMatcher = Pattern.compile("https?://[^\\s]+").matcher(email);
        org.junit.jupiter.api.Assertions.assertTrue(linkMatcher.find(), "verification link should be included in the development email");
        String rawToken = URI.create(linkMatcher.group()).getQuery().substring("token=".length());
        String storedHash = jdbc.queryForObject("SELECT token_hash FROM email_verification_tokens WHERE email = ?", String.class, "asha.registration@example.edu");
        org.junit.jupiter.api.Assertions.assertNotEquals(rawToken, storedHash);

        mvc.perform(post("/login").with(csrf())
                .param("email", "asha.registration@example.edu")
                .param("password", "BetterLearning2026"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void verificationIsSingleUseAndVerifiedUserCanLogIn() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Ravi Kapoor")
                .param("email", "ravi.verification@example.edu")
                .param("password", "LearningSecure2026")
                .param("confirmPassword", "LearningSecure2026")
                .param("termsAccepted", "true"))
            .andExpect(status().is3xxRedirection());

        String email = outbox.latest().getFirst().text();
        Matcher matcher = Pattern.compile("https?://[^\\s]+").matcher(email);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find());
        String token = URI.create(matcher.group()).getQuery().substring("token=".length());

        mvc.perform(get("/verify-email").param("token", token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Email confirmed")));
        org.junit.jupiter.api.Assertions.assertNotNull(jdbc.queryForObject(
            "SELECT email_verified_at FROM users WHERE email = ?", java.sql.Timestamp.class, "ravi.verification@example.edu"));

        mvc.perform(get("/verify-email").param("token", token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Already confirmed")));

        var loginResult = mvc.perform(post("/login").with(csrf())
                .param("email", "ravi.verification@example.edu")
                .param("password", "LearningSecure2026"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);
        mvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Good to see you, <span>Ravi Kapoor</span>.")))
            .andExpect(content().string(containsString("No sessions yet.")));
        mvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/?loggedOut"));
    }

    @Test
    void invalidPasswordAndTermsDoNotCreateAnAccount() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Mina Shah")
                .param("email", "mina.invalid@example.edu")
                .param("password", "weak")
                .param("confirmPassword", "different")
                .param("termsAccepted", "false"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Passwords do not match.")))
            .andExpect(content().string(containsString("Accept the terms and privacy policy")));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "mina.invalid@example.edu"));
    }

    @Test
    void duplicateEmailIsRejectedWithoutCreatingASecondRecord() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Asha Mehta")
                .param("email", "duplicate.registration@example.edu")
                .param("password", "BetterLearning2026")
                .param("confirmPassword", "BetterLearning2026")
                .param("termsAccepted", "true"))
            .andExpect(status().is3xxRedirection());

        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Other Student")
                .param("email", "DUPLICATE.REGISTRATION@example.edu")
                .param("password", "DifferentPassword2026")
                .param("confirmPassword", "DifferentPassword2026")
                .param("termsAccepted", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("An account with this email address already exists")));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "duplicate.registration@example.edu"));
    }
}
