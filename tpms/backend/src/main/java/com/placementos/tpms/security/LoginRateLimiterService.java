package com.placementos.tpms.security;

import com.placementos.tpms.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60; // 15 minutes

    private static final Set<String> EXEMPT_DEMO_EMAILS = Set.of(
            "candidate.student@rknec.edu",
            "student1@rknec.edu",
            "admin@rknec.edu",
            "coord_user@rknec.edu"
    );

    public boolean isDemoExempt(String identifier) {
        if (identifier == null) return false;
        return EXEMPT_DEMO_EMAILS.contains(identifier.trim().toLowerCase());
    }

    private static class AttemptRecord {
        int failedAttempts;
        long lastAttemptEpochSeconds;

        AttemptRecord(int count, long epochSeconds) {
            this.failedAttempts = count;
            this.lastAttemptEpochSeconds = epochSeconds;
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void checkLoginAllowed(String identifier) {
        if (identifier == null || isDemoExempt(identifier)) return;
        String key = identifier.trim().toLowerCase();
        AttemptRecord record = attempts.get(key);
        if (record == null) return;

        long now = Instant.now().getEpochSecond();
        if (now - record.lastAttemptEpochSeconds > LOCKOUT_DURATION_SECONDS) {
            // Lockout period expired, reset counter
            attempts.remove(key);
            return;
        }

        if (record.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            long remainingMinutes = Math.max(1, (LOCKOUT_DURATION_SECONDS - (now - record.lastAttemptEpochSeconds)) / 60);
            throw new AppException(
                    "Too many failed login attempts. Account temporarily locked. Please try again in " + remainingMinutes + " minutes.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public void recordFailedAttempt(String identifier) {
        if (identifier == null || isDemoExempt(identifier)) return;
        String key = identifier.trim().toLowerCase();
        long now = Instant.now().getEpochSecond();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.lastAttemptEpochSeconds > LOCKOUT_DURATION_SECONDS)) {
                return new AttemptRecord(1, now);
            }
            existing.failedAttempts++;
            existing.lastAttemptEpochSeconds = now;
            return existing;
        });
    }

    public void recordSuccess(String identifier) {
        if (identifier == null) return;
        attempts.remove(identifier.trim().toLowerCase());
    }

    public void clearAll() {
        attempts.clear();
    }
}
