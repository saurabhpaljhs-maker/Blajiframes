package com.balaji.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute Force Protection:
 * After 5 failed login attempts, blocks the IP for 15 minutes.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final int    MAX_ATTEMPTS   = 5;
    private static final int    BLOCK_MINUTES  = 15;

    // ip -> [failCount, blockedUntil]
    private final Map<String, int[]>         attempts   = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedUntil = new ConcurrentHashMap<>();

    public void loginSucceeded(String ip) {
        attempts.remove(ip);
        blockedUntil.remove(ip);
        log.info("Login success from IP: {}", ip);
    }

    public void loginFailed(String ip) {
        int[] count = attempts.getOrDefault(ip, new int[]{0});
        count[0]++;
        attempts.put(ip, count);

        if (count[0] >= MAX_ATTEMPTS) {
            blockedUntil.put(ip, LocalDateTime.now().plusMinutes(BLOCK_MINUTES));
            log.warn("IP blocked for {} min after {} failed attempts: {}", BLOCK_MINUTES, MAX_ATTEMPTS, ip);
        }
    }

    public boolean isBlocked(String ip) {
        LocalDateTime until = blockedUntil.get(ip);
        if (until == null) return false;

        if (LocalDateTime.now().isAfter(until)) {
            // Block expired — clear
            blockedUntil.remove(ip);
            attempts.remove(ip);
            return false;
        }
        return true;
    }

    public long minutesRemaining(String ip) {
        LocalDateTime until = blockedUntil.get(ip);
        if (until == null) return 0;
        return java.time.Duration.between(LocalDateTime.now(), until).toMinutes() + 1;
    }
}
