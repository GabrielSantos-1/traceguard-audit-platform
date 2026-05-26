package com.traceguard.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ResilientRateLimiterService implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(ResilientRateLimiterService.class);

    private final InMemoryRateLimiterService inMemoryRateLimiterService;
    private final RedisRateLimiterService redisRateLimiterService;
    private final boolean redisEnabled;
    private final MeterRegistry meterRegistry;

    public ResilientRateLimiterService(
            InMemoryRateLimiterService inMemoryRateLimiterService,
            ObjectProvider<RedisRateLimiterService> redisRateLimiterServiceProvider,
            @Value("${traceguard.rate-limit.redis.enabled:false}") boolean redisEnabled,
            MeterRegistry meterRegistry) {
        this.inMemoryRateLimiterService = inMemoryRateLimiterService;
        this.redisRateLimiterService = redisRateLimiterServiceProvider.getIfAvailable();
        this.redisEnabled = redisEnabled;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RateLimitDecision check(String key, RateLimitPolicy policy) {
        if (!redisEnabled || redisRateLimiterService == null) {
            return inMemoryRateLimiterService.check(key, policy);
        }

        try {
            return redisRateLimiterService.check(key, policy);
        } catch (Exception ex) {
            meterRegistry.counter(
                    "traceguard.ratelimit.redis.error",
                    "bucket", policy.bucket(),
                    "source", "redis").increment();
            meterRegistry.counter(
                    "traceguard.ratelimit.fallback.memory",
                    "bucket", policy.bucket(),
                    "source", "memory").increment();
            log.warn(
                    "event=RATE_LIMIT_REDIS_FALLBACK bucket={} reason={} fallback=memory",
                    policy.bucket(),
                    ex.getClass().getSimpleName());
            return inMemoryRateLimiterService.check(key, policy);
        }
    }
}
