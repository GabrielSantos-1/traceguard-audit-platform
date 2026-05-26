package com.traceguard.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "traceguard.rate-limit.redis", name = "enabled", havingValue = "true")
public class RedisRateLimiterService implements RateLimiterService {

    private static final long WINDOW_SECONDS = 60L;

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitDecision check(String key, RateLimitPolicy policy) {
        String redisKey = "rate:" + policy.bucket() + ":" + normalizeKey(key);
        Long used = redisTemplate.opsForValue().increment(redisKey);
        if (used == null) {
            throw new IllegalStateException("Redis did not return increment value");
        }
        if (used == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
        }

        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        long retryAfter = ttl != null && ttl > 0 ? ttl : 1L;
        int limit = policy.limitPerMinute();
        if (used <= limit) {
            return new RateLimitDecision(true, limit, Math.max(limit - used.intValue(), 0), 0, "redis");
        }
        return new RateLimitDecision(false, limit, 0, retryAfter, "redis");
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "unknown";
        }
        return key.trim();
    }
}

