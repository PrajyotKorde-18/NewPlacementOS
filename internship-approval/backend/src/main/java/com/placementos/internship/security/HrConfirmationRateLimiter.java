package com.placementos.internship.security;

import com.placementos.internship.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class HrConfirmationRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long WINDOW_SECONDS = 60; // 1 minute window

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();

    public String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "unknown";
    }

    public void checkRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            return;
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - WINDOW_SECONDS;

        ConcurrentLinkedQueue<Long> timestamps = ipRequestTimestamps.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());

        // Purge expired timestamps
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
            throw new AppException("Too many requests from this IP. Please try again in a moment.", HttpStatus.TOO_MANY_REQUESTS);
        }

        timestamps.add(now);
    }
}
