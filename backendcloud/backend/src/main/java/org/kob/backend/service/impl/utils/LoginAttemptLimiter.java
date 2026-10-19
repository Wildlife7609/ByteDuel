package org.kob.backend.service.impl.utils;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Limits failed logins to stop password brute-forcing: MAX_FAILURES failures for one username within WINDOW locks it for LOCK_DURATION
// LoginAttemptListener updates the counts from Spring Security's login events; the lock is passed to Spring Security through UserDetailsImpl.isAccountNonLocked(),
// which rejects a locked account before checking the password (throwing LockedException)
// Counted per username rather than per IP: requests pass through Cloudflare and Caddy, so backend does not see the user's real IP
// Counts live only in memory: backend runs a single instance, and resetting on restart is fine
@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    public static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 100_000; // keeps random usernames from filling up memory

    // firstFailure: when the current window started; lockedUntil: when the lock ends, or null if not locked
    private record Attempt(int failures, Instant firstFailure, Instant lockedUntil) {
        boolean expired(Instant now) {
            if (lockedUntil != null)
                return !now.isBefore(lockedUntil);
            return !now.isBefore(firstFailure.plus(WINDOW));
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        Attempt attempt = attempts.get(username);
        return attempt != null && attempt.lockedUntil() != null && Instant.now().isBefore(attempt.lockedUntil());
    }

    public void recordFailure(String username) {
        Instant now = Instant.now();
        if (attempts.size() >= MAX_ENTRIES)
            attempts.values().removeIf(attempt -> attempt.expired(now));
        attempts.compute(username, (key, attempt) -> {
            if (attempt == null || attempt.expired(now))
                attempt = new Attempt(0, now, null); // the previous window expired, start over
            int failures = attempt.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempt(failures, attempt.firstFailure(), lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }
}
