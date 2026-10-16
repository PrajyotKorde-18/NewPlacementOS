package com.placementos.internship.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NocVerificationRateLimiter {

    private static final int MAX_REQUESTS = 30;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final Map<String, List<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();

    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        long now = System.currentTimeMillis();
        List<Long> timestamps = ipRequestTimestamps.computeIfAbsent(ip, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > WINDOW_MS);
            if (timestamps.size() >= MAX_REQUESTS) {
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }
}
