package com.skillswap.service;

import com.skillswap.dto.RegistrationForm;
import com.skillswap.entity.EmailVerificationTokenEntity;
import com.skillswap.entity.UserEntity;
import com.skillswap.repository.EmailVerificationTokenRepository;
import com.skillswap.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RegistrationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final OutboundMailService mail;
    private final String baseUrl;
    private final Duration verificationLifetime;

    public RegistrationService(UserRepository users, EmailVerificationTokenRepository tokens,
                               PasswordEncoder passwordEncoder, OutboundMailService mail,
                               @Value("${skillswap.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${skillswap.security.verification-token-hours:24}") long verificationHours) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.baseUrl = StringUtils.hasText(baseUrl) ? baseUrl.replaceAll("/+$", "") : "http://localhost:8080";
        this.verificationLifetime = Duration.ofHours(verificationHours);
    }

    @Transactional
    public void register(RegistrationForm form) {
        String email = form.getEmail().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw new EmailAlreadyInUseException();

        UserEntity user = new UserEntity(email, passwordEncoder.encode(form.getPassword()),
            form.getFullName(), form.getCollege(), form.getDepartment(),
            form.getSemester() == null ? null : form.getSemester().byteValue(), Instant.now());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyInUseException();
        }

        Instant now = Instant.now();
        tokens.invalidateActiveForUser(user.getId(), now);
        byte[] randomToken = new byte[32];
        RANDOM.nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        tokens.save(new EmailVerificationTokenEntity(user, email, TokenDigests.sha256(token),
            now.plus(verificationLifetime), now));

        String link = baseUrl + "/verify-email?token=" + token;
        mail.send(email, "Verify your SkillSwap email",
            "Hi " + user.getFullName() + ",\n\n" +
            "Verify your email address to finish creating your SkillSwap account:\n" + link + "\n\n" +
            "This link expires in " + verificationLifetime.toHours() + " hours. If you did not create this account, ignore this message.\n\n" +
            "SkillSwap — Learn Together. Grow Together.");
    }

    @Transactional
    public EmailVerificationResult verify(String rawToken) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 128) {
            return EmailVerificationResult.INVALID_OR_EXPIRED;
        }
        var found = tokens.findByHashWithUser(TokenDigests.sha256(rawToken.strip()));
        if (found.isEmpty()) return EmailVerificationResult.INVALID_OR_EXPIRED;
        var token = found.get();
        var user = token.getUser();
        if (user.isEmailVerified()) return EmailVerificationResult.ALREADY_VERIFIED;
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)
            || !token.getEmail().equalsIgnoreCase(user.getEmail())) {
            return EmailVerificationResult.INVALID_OR_EXPIRED;
        }
        token.markUsed(now);
        user.setEmailVerifiedAt(now);
        return EmailVerificationResult.VERIFIED;
    }

}
