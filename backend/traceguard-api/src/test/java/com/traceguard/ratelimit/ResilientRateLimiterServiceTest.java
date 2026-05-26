package com.traceguard.ratelimit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ResilientRateLimiterServiceTest {

    @Test
    void deve_usar_in_memory_quando_redis_desabilitado() {
        InMemoryRateLimiterService inMemory = mock(InMemoryRateLimiterService.class);
        RedisRateLimiterService redis = mock(RedisRateLimiterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisRateLimiterService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(inMemory.check("127.0.0.1", new RateLimitPolicy(5, "login")))
                .thenReturn(new RateLimitDecision(true, 5, 4, 0, "memory"));

        ResilientRateLimiterService service = new ResilientRateLimiterService(
                inMemory,
                provider,
                false,
                new SimpleMeterRegistry());

        RateLimitDecision decision = service.check("127.0.0.1", new RateLimitPolicy(5, "login"));

        assertThat(decision.source()).isEqualTo("memory");
        verifyNoInteractions(redis);
    }

    @Test
    void deve_fazer_fallback_para_in_memory_quando_redis_falha() {
        InMemoryRateLimiterService inMemory = mock(InMemoryRateLimiterService.class);
        RedisRateLimiterService redis = mock(RedisRateLimiterService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisRateLimiterService> provider = mock(ObjectProvider.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimitPolicy policy = new RateLimitPolicy(10, "refresh");

        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.check("10.0.0.1", policy)).thenThrow(new RuntimeException("redis down"));
        when(inMemory.check("10.0.0.1", policy))
                .thenReturn(new RateLimitDecision(true, 10, 9, 0, "memory"));

        ResilientRateLimiterService service = new ResilientRateLimiterService(
                inMemory,
                provider,
                true,
                meterRegistry);

        RateLimitDecision decision = service.check("10.0.0.1", policy);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.source()).isEqualTo("memory");
        assertThat(meterRegistry.get("traceguard.ratelimit.redis.error")
                .tag("bucket", "refresh")
                .counter()
                .count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("traceguard.ratelimit.fallback.memory")
                .tag("bucket", "refresh")
                .counter()
                .count()).isEqualTo(1.0d);
        verify(inMemory).check("10.0.0.1", policy);
    }
}

