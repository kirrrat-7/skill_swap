package com.skillswap.config;

import com.skillswap.repository.UserRepository;
import com.skillswap.security.AuthVersionValidationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder(@Value("${skillswap.security.bcrypt-strength:12}") int strength) {
        if (strength < 10 || strength > 14) {
            throw new IllegalStateException("BCrypt strength must be between 10 and 14.");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email.strip())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, UserDetailsService userDetailsService,
                                            PasswordEncoder passwordEncoder, UserRepository users) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        http
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/skills", "/about", "/contact", "/privacy", "/terms",
                    "/register", "/login", "/verify-email", "/verify-email/sent",
                    "/forgot-password", "/forgot-password/sent", "/reset-password", "/dev/outbox",
                    "/api/public/home", "/css/**", "/js/**", "/images/**", "/icons/**", "/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/?loggedOut").permitAll())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; font-src 'self'; " +
                    "connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .addFilterAfter(new AuthVersionValidationFilter(users),
                org.springframework.security.web.context.SecurityContextHolderFilter.class);
        return http.build();
    }
}
