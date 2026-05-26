package com.traceguard.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryRateLimiterService implements RateLimiterService {

    private static final long WINDOW_SECONDS = 60L;

    private final ConcurrentHashMap<String, WindowState> store = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision check(String key, RateLimitPolicy policy) {
        long now = Instant.now().getEpochSecond();
        String bucketKey = policy.bucket() + ":" + normalizeKey(key);
        WindowState state = store.compute(bucketKey, (k, current) -> {
            if (current == null || now >= current.windowStartEpochSecond + WINDOW_SECONDS) {
                return new WindowState(now, new AtomicInteger(1));
            }
            current.count.incrementAndGet();
            return current;
        });

        int used = state.count.get();
        int limit = policy.limitPerMinute();
        if (used <= limit) {
            return new RateLimitDecision(true, limit, Math.max(limit - used, 0), 0, "memory");
        }
        long retryAfter = (state.windowStartEpochSecond + WINDOW_SECONDS) - now;
        return new RateLimitDecision(false, limit, 0, Math.max(retryAfter, 1), "memory");
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "unknown";
        }
        return key.trim();
    }

    private static final class WindowState {
        private final long windowStartEpochSecond;
        private final AtomicInteger count;

        private WindowState(long windowStartEpochSecond, AtomicInteger count) {
            this.windowStartEpochSecond = windowStartEpochSecond;
            this.count = count;
        }
    }
}
