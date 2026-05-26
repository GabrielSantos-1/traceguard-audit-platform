package com.traceguard.ratelimit;

public record RateLimitPolicy(
        int limitPerMinute,
        String bucket
) {
}
