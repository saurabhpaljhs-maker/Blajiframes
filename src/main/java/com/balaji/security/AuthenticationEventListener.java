package com.balaji.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String ip = getClientIP();
        loginAttemptService.loginSucceeded(ip);
        log.info("✅ Login successful: {} from IP: {}",
                event.getAuthentication().getName(), ip);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String ip = getClientIP();
        loginAttemptService.loginFailed(ip);
        log.warn("❌ Login failed from IP: {} | Reason: {}",
                ip, event.getException().getMessage());
    }

    private String getClientIP() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            return (forwarded != null) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
