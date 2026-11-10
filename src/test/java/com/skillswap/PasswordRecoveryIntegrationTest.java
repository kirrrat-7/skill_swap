package com.skillswap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillswap.service.DevMailOutboxService;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordRecoveryIntegrationTest {
    private static final String EMAIL = "recovery.student@example.edu";
    private static final String INITIAL_PASSWORD = "SecureStarter2026";
    private static final String NEW_PASSWORD = "BrandNewPassword2026";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DevMailOutboxService outbox;

    @BeforeEach
    void clearOutbox() { outbox.clear(); }

    @AfterEach
    void cleanTestData() {
        jdbc.update("DELETE FROM users WHERE email = ?", EMAIL);
        jdbc.update("DELETE FROM security_events WHERE event_type = 'PASSWORD_RESET_REQUEST'");
        jdbc.update("DELETE FROM rate_limit_buckets WHERE bucket_key = ?",
            com.skillswap.service.TokenDigests.sha256("127.0.0.1"));
        outbox.clear();
    }

    @Test
    void oneTimeResetChangesPasswordAndInvalidatesExistingSessions() throws Exception {
        registerAndVerify(EMAIL, INITIAL_PASSWORD);
        var login = mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("password", INITIAL_PASSWORD))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard")).andReturn();
        MockHttpSession oldSession = (MockHttpSession) login.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(oldSession);
        mvc.perform(get("/dashboard").session(oldSession)).andExpect(status().isOk());

        outbox.clear();
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mvc.perform(post("/forgot-password").with(csrf()).param("email", EMAIL))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password/sent"));
        mvc.perform(get("/forgot-password/sent"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("whether or not an account exists")));
        org.junit.jupiter.api.Assertions.assertEquals(1, outbox.latest().size());

        String resetEmail = outbox.latest().getFirst().text();
        String rawToken = extractToken(resetEmail, "/reset-password?token=");
        String storedHash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id = (SELECT id FROM users WHERE email = ?)", String.class, EMAIL);
        org.junit.jupiter.api.Assertions.assertNotEquals(rawToken, storedHash);
        mvc.perform(get("/reset-password").param("token", rawToken))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Set a new password.")));

        mvc.perform(post("/reset-password").with(csrf()).param("token", rawToken)
                .param("password", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?reset"));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
            "SELECT auth_version FROM users WHERE email = ?", Integer.class, EMAIL));
        org.junit.jupiter.api.Assertions.assertNotNull(jdbc.queryForObject(
            "SELECT used_at FROM password_reset_tokens WHERE token_hash = ?", java.sql.Timestamp.class, storedHash));

        mvc.perform(get("/dashboard").session(oldSession))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/reset-password").with(csrf()).param("token", rawToken)
                .param("password", "AnotherPassword2026").param("confirmPassword", "AnotherPassword2026"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("This reset link is unavailable.")));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
            "SELECT auth_version FROM users WHERE email = ?", Integer.class, EMAIL));

        mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("password", INITIAL_PASSWORD))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("password", NEW_PASSWORD))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void resetRequestsAreGenericAndLimitedByClientAddress() throws Exception {
        registerAndVerify(EMAIL, INITIAL_PASSWORD);
        outbox.clear();
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/forgot-password").with(csrf()).param("email", EMAIL))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password/sent"));
        }
        org.junit.jupiter.api.Assertions.assertEquals(5, outbox.latest().size());
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "unknown.student@example.edu"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password/sent"));
        org.junit.jupiter.api.Assertions.assertEquals(5, outbox.latest().size());
    }

    private void registerAndVerify(String email, String password) throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Kiran Rao")
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", password)
                .param("termsAccepted", "true"))
            .andExpect(status().is3xxRedirection());
        String verifyEmail = outbox.latest().getFirst().text();
        String verificationToken = extractToken(verifyEmail, "/verify-email?token=");
        mvc.perform(get("/verify-email").param("token", verificationToken))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Email confirmed")));
    }

    private static String extractToken(String message, String suffix) {
        Matcher matcher = Pattern.compile("https?://[^\\s]+").matcher(message);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find(), "email should contain a one-time link");
        String url = URI.create(matcher.group()).toString();
        int start = url.indexOf(suffix);
        org.junit.jupiter.api.Assertions.assertTrue(start >= 0, "email should contain the expected link");
        return url.substring(start + suffix.length());
    }
}
