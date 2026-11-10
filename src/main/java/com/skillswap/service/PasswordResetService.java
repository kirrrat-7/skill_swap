package com.skillswap.service;

import com.skillswap.entity.PasswordResetTokenEntity;
import com.skillswap.repository.PasswordResetTokenRepository;
import com.skillswap.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_REQUESTS_PER_HOUR_PER_IP = 5;
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final OutboundMailService mail;
    private final JdbcTemplate jdbc;
    private final String baseUrl;
    private final Duration tokenLifetime;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder, OutboundMailService mail,
                                JdbcTemplate jdbc,
                                @Value("${skillswap.base-url:http://localhost:8080}") String baseUrl,
                                @Value("${skillswap.security.reset-token-minutes:30}") long tokenMinutes) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.jdbc = jdbc;
        this.baseUrl = StringUtils.hasText(baseUrl) ? baseUrl.replaceAll("/+$", "") : "http://localhost:8080";
        this.tokenLifetime = Duration.ofMinutes(tokenMinutes);
    }

    /** Always returns the same controller response, whether the address exists or is rate-limited. */
    @Transactional
    public void requestReset(String address, String remoteAddress) {
        String ip = safeIp(remoteAddress);
        boolean limited = incrementRateLimit(ip);
        jdbc.update("""
            INSERT INTO security_events (user_id, email, event_type, ip_address, user_agent, details, created_at)
            VALUES (NULL, NULL, 'PASSWORD_RESET_REQUEST', ?, NULL, NULL, UTC_TIMESTAMP(6))
            """, ip);
        if (limited) return;
        if (!StringUtils.hasText(address) || address.length() > 254) return;

        var user = users.findByEmailIgnoreCase(address.strip().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || !user.hasLocalPassword() || !user.isEmailVerified()
            || user.getStatus() != com.skillswap.entity.UserEntity.Status.ACTIVE) return;

        Instant now = Instant.now();
        tokens.invalidateActiveForUser(user.getId(), now);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetTokenEntity(user, TokenDigests.sha256(raw),
            now.plus(tokenLifetime), ip, now));
        String link = baseUrl + "/reset-password?token=" + raw;
        mail.send(user.getEmail(), "Reset your SkillSwap password",
            "A request was made to reset the password for your SkillSwap account.\n\n" +
            "If you made this request, choose a new password here:\n" + link + "\n\n" +
            "This link expires in " + tokenLifetime.toMinutes() + " minutes and can only be used once. " +
            "If you did not request this, ignore this email; your password will not change.\n\n" +
            "SkillSwap — Learn Together. Grow Together.");
    }

    @Transactional(readOnly = true)
    public boolean isUsable(String rawToken) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 128) return false;
        return tokens.findByHash(TokenDigests.sha256(rawToken.strip()))
            .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(Instant.now())
                && token.getUser().isEmailVerified() && token.getUser().hasLocalPassword())
            .isPresent();
    }

    @Transactional
    public boolean reset(String rawToken, String newPassword) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 128) return false;
        var found = tokens.lockByHashWithUser(TokenDigests.sha256(rawToken.strip()));
        if (found.isEmpty()) return false;
        PasswordResetTokenEntity token = found.get();
        var user = token.getUser();
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)
            || !user.isEmailVerified() || !user.hasLocalPassword()
            || user.getStatus() != com.skillswap.entity.UserEntity.Status.ACTIVE) return false;
        user.resetPassword(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        return true;
    }

    private boolean incrementRateLimit(String ip) {
        String key = TokenDigests.sha256(ip);
        jdbc.update("""
            INSERT INTO rate_limit_buckets (bucket_key, window_started_at, request_count, created_at, updated_at)
            VALUES (?, UTC_TIMESTAMP(6), 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
              request_count = CASE
                WHEN window_started_at <= UTC_TIMESTAMP(6) - INTERVAL 1 HOUR THEN 1
                ELSE request_count + 1 END,
              window_started_at = CASE
                WHEN window_started_at <= UTC_TIMESTAMP(6) - INTERVAL 1 HOUR THEN UTC_TIMESTAMP(6)
                ELSE window_started_at END,
              updated_at = UTC_TIMESTAMP(6)
            """, key);
        Integer count = jdbc.queryForObject(
            "SELECT request_count FROM rate_limit_buckets WHERE bucket_key = ?", Integer.class, key);
        return count == null || count > MAX_REQUESTS_PER_HOUR_PER_IP;
    }

    private static String safeIp(String value) {
        if (!StringUtils.hasText(value)) return "unknown";
        String ip = value.strip();
        return ip.length() <= 45 ? ip : "unknown";
    }
}
