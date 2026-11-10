package com.skillswap.security;

import com.skillswap.entity.UserEntity;
import com.skillswap.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revokes authenticated sessions after password changes, suspension, or account deletion. */
public class AuthVersionValidationFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public AuthVersionValidationFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof UserEntity principal) {
            UserEntity current = users.findById(principal.getId()).orElse(null);
            if (current == null || current.getStatus() != UserEntity.Status.ACTIVE
                || current.getAuthVersion() != principal.getAuthVersion()) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    try { session.invalidate(); }
                    catch (IllegalStateException ignored) { /* already invalidated */ }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
