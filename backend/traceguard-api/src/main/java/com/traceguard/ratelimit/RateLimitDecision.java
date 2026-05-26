package com.traceguard.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        long retryAfterSeconds,
        String source
) {
    public RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds) {
        this(allowed, limit, remaining, retryAfterSeconds, "memory");
    }
}
