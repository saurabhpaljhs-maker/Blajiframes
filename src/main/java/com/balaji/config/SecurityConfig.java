package com.balaji.config;

import com.balaji.security.CustomUserDetailsService;
import com.balaji.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService      loginAttemptService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ── SABSE PEHLE static resources allow karo ──
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**",
                    "/static/**",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()
                // ── Public pages ──
                .requestMatchers(
                    "/",
                    "/api/frame/**",
                    "/api/payment/**",
                    "/api/order/place",
                    "/order/confirmation/**",
                    "/h2-console/**",
                    "/login",
                    "/login/**",
                    "/error"
                ).permitAll()
                // ── Admin only ──
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**", "/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(fo -> fo.sameOrigin())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://checkout.razorpay.com https://fonts.googleapis.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: blob:; " +
                    "frame-src https://api.razorpay.com; " +
                    "connect-src 'self' https://api.razorpay.com;"
                ))
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint customAuthEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Unauthorized\",\"message\":\"Login required\"}");
            } else {
                response.sendRedirect("/login");
            }
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                AccessDeniedException accessDeniedException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
            } else {
                response.sendRedirect("/login?denied=true");
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) -> {
            String ip = getClientIP(request);
            loginAttemptService.loginSucceeded(ip);
            response.sendRedirect("/admin/orders");
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException exception) -> {
            String ip = getClientIP(request);
            if (loginAttemptService.isBlocked(ip)) {
                long mins = loginAttemptService.minutesRemaining(ip);
                response.sendRedirect("/login?blocked=true&minutes=" + mins);
            } else {
                loginAttemptService.loginFailed(ip);
                response.sendRedirect("/login?error=true");
            }
        };
    }

    private String getClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null)
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
