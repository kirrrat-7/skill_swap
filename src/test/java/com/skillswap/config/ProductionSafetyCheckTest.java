package com.skillswap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyCheckTest {
    @Test
    void productionConfigurationMustBeExplicitAndMustNotEchoSecretValues() {
        var env = new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/skillswap")
            .withProperty("spring.datasource.username", "local-user")
            .withProperty("spring.datasource.password", "local-password")
            .withProperty("skillswap.base-url", "http://localhost:8080")
            .withProperty("skillswap.security.encryption-key", "not-a-key")
            .withProperty("server.servlet.session.cookie.secure", "false")
            .withProperty("skillswap.security.require-https", "false")
            .withProperty("skillswap.mail.dev-outbox", "true")
            .withProperty("spring.mail.host", "")
            .withProperty("spring.mail.username", "")
            .withProperty("spring.mail.password", "")
            .withProperty("skillswap.mail.from", "")
            .withProperty("spring.mail.properties.mail.smtp.starttls.enable", "false");

        var problems = new ProductionSafetyCheck(env).validate();
        assertTrue(problems.contains("SKILLSWAP_DB_URL (remote MySQL 8+)"));
        assertTrue(problems.contains("SKILLSWAP_ENCRYPTION_KEY (base64-encoded 32 random bytes)"));
        assertTrue(problems.contains("SKILLSWAP_SMTP_HOST"));
        String message = String.join(" ", problems);
        assertTrue(!message.contains("local-password") && !message.contains("not-a-key"));
    }

    @Test
    void productionConfigurationAcceptsRemoteMysqlHttpsThirtyTwoByteKeyAndTlsMail() {
        var env = new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:mysql://mysql.internal:3306/skillswap")
            .withProperty("spring.datasource.username", "skillswap")
            .withProperty("spring.datasource.password", "set-by-secret-manager")
            .withProperty("skillswap.base-url", "https://skillswap.example.com")
            .withProperty("skillswap.security.encryption-key", Base64.getEncoder().encodeToString(new byte[32]))
            .withProperty("server.servlet.session.cookie.secure", "true")
            .withProperty("skillswap.security.require-https", "true")
            .withProperty("skillswap.mail.dev-outbox", "false")
            .withProperty("spring.mail.host", "smtp.example.com")
            .withProperty("spring.mail.username", "sender")
            .withProperty("spring.mail.password", "mail-secret")
            .withProperty("skillswap.mail.from", "noreply@example.com")
            .withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true");

        assertEquals(java.util.List.of(), new ProductionSafetyCheck(env).validate());
    }
}
