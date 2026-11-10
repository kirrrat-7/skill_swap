package com.skillswap.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Refuses to boot a production profile with development defaults or missing critical secrets. */
@Component
@Profile("prod")
public class ProductionSafetyCheck implements ApplicationRunner {
    private final Environment environment;

    public ProductionSafetyCheck(Environment environment) { this.environment = environment; }

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = validate();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Production configuration is incomplete. Set: " + String.join(", ", missing));
        }
    }

    List<String> validate() {
        List<String> problems = new ArrayList<>();
        String databaseUrl = property("spring.datasource.url");
        if (!databaseUrl.startsWith("jdbc:mysql:") || databaseUrl.contains("localhost") || databaseUrl.contains("127.0.0.1")) {
            problems.add("SKILLSWAP_DB_URL (remote MySQL 8+)");
        }
        if (!StringUtils.hasText(property("spring.datasource.username"))) problems.add("SKILLSWAP_DB_USERNAME");
        if (!StringUtils.hasText(property("spring.datasource.password"))) problems.add("SKILLSWAP_DB_PASSWORD");

        String site = property("skillswap.base-url");
        try {
            URI uri = URI.create(site);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                problems.add("SKILLSWAP_BASE_URL (public HTTPS origin)");
            }
        } catch (IllegalArgumentException invalid) {
            problems.add("SKILLSWAP_BASE_URL (public HTTPS origin)");
        }

        String key = property("skillswap.security.encryption-key");
        try {
            if (Base64.getDecoder().decode(key).length != 32) problems.add("SKILLSWAP_ENCRYPTION_KEY (base64-encoded 32 random bytes)");
        } catch (IllegalArgumentException invalid) {
            problems.add("SKILLSWAP_ENCRYPTION_KEY (base64-encoded 32 random bytes)");
        }
        if (!Boolean.parseBoolean(property("server.servlet.session.cookie.secure"))) problems.add("SKILLSWAP_COOKIE_SECURE=true");
        if (!Boolean.parseBoolean(property("skillswap.security.require-https"))) problems.add("SKILLSWAP_REQUIRE_HTTPS=true");
        if (Boolean.parseBoolean(property("skillswap.mail.dev-outbox"))) problems.add("SKILLSWAP_DEV_OUTBOX=false");

        if (!StringUtils.hasText(property("spring.mail.host"))) problems.add("SKILLSWAP_SMTP_HOST");
        if (!StringUtils.hasText(property("spring.mail.username"))) problems.add("SKILLSWAP_SMTP_USERNAME");
        if (!StringUtils.hasText(property("spring.mail.password"))) problems.add("SKILLSWAP_SMTP_PASSWORD");
        if (!StringUtils.hasText(property("skillswap.mail.from"))) problems.add("SKILLSWAP_MAIL_FROM");
        if (!Boolean.parseBoolean(property("spring.mail.properties.mail.smtp.starttls.enable"))) problems.add("SMTP STARTTLS must be enabled");

        return List.copyOf(problems);
    }

    private String property(String key) {
        return environment.getProperty(key, "").strip();
    }
}
