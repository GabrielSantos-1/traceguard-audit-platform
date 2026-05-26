package com.traceguard.ratelimit;

public interface RateLimiterService {

    RateLimitDecision check(String key, RateLimitPolicy policy);
}

