package com.balaji.config;

import com.balaji.security.CustomUserDetailsService;
import com.balaji.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService      loginAttemptService;

    @Value("${app.admin.username:balaji_admin}")
    private String adminUsername;

    // ── BCrypt Password Encoder ───────────────────────────────────────────────
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 12 — secure
    }

    // ── Auth Provider ─────────────────────────────────────────────────────────
    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── Auth Manager ──────────────────────────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Main Security Filter Chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── URL Authorization ─────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public — customer-facing pages
                .requestMatchers(
                    "/",
                    "/css/**", "/js/**", "/images/**", "/uploads/**",
                    "/api/frame/**",
                    "/api/payment/**",
                    "/api/order/place",
                    "/order/confirmation/**",
                    "/h2-console/**",
                    "/login", "/login/**",
                    "/error"
                ).permitAll()
                // Admin — requires ROLE_ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Anything else — must be authenticated
                .anyRequest().authenticated()
            )

            // ── Form Login ────────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            )

            // ── Logout ────────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .clearAuthentication(true)
                .permitAll()
            )

            // ── Session Management ─────────────────────────────────────────────
            .sessionManagement(session -> session
                .maximumSessions(1)                 // 1 session per admin at a time
                .maxSessionsPreventsLogin(false)    // new login kicks old session
            )

            // ── CSRF ──────────────────────────────────────────────────────────
            // Cookie-based CSRF — works well with Thymeleaf + REST
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/**",          // REST APIs — use Razorpay sig instead
                    "/h2-console/**"    // Dev only
                )
            )

            // ── Security Headers ──────────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(fo -> fo.sameOrigin())           // H2 console support
                .xssProtection(xss -> xss.disable())           // modern browsers handle XSS
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://checkout.razorpay.com https://fonts.googleapis.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data: https://images.unsplash.com; " +
                        "frame-src https://api.razorpay.com; " +
                        "connect-src 'self' https://api.razorpay.com;"
                    )
                )
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )

            // ── Exception Handling ────────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((HttpServletRequest req, HttpServletResponse res, org.springframework.security.core.AuthenticationException e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Login required\"}");
                    } else {
                        res.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                    } else {
                        res.sendRedirect("/login?denied=true");
                    }
                })
            );

        return http.build();
    }

    // ── Success Handler: check brute force, then redirect ─────────────────────
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest req, HttpServletResponse res,
                org.springframework.security.core.Authentication auth) -> {
            String ip = getClientIP(req);
            loginAttemptService.loginSucceeded(ip);
            res.sendRedirect("/admin/orders");
        };
    }

    // ── Failure Handler: track failed attempts, block if needed ───────────────
    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (HttpServletRequest req, HttpServletResponse res, Exception ex) -> {
            String ip = getClientIP(req);

            if (loginAttemptService.isBlocked(ip)) {
                long mins = loginAttemptService.minutesRemaining(ip);
                res.sendRedirect("/login?blocked=true&minutes=" + mins);
            } else {
                loginAttemptService.loginFailed(ip);
                res.sendRedirect("/login?error=true");
            }
        };
    }

    private String getClientIP(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
