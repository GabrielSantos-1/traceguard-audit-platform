package com.traceguard.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisRateLimiterServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisRateLimiterService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RedisRateLimiterService(redisTemplate);
    }

    @Test
    void deve_permitir_quando_contador_abaixo_limite() {
        when(valueOperations.increment("rate:login:127.0.0.1")).thenReturn(1L);
        when(redisTemplate.getExpire("rate:login:127.0.0.1", TimeUnit.SECONDS)).thenReturn(60L);

        RateLimitDecision decision = service.check("127.0.0.1", new RateLimitPolicy(5, "login"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(5);
        assertThat(decision.remaining()).isEqualTo(4);
        assertThat(decision.source()).isEqualTo("redis");
        verify(redisTemplate).expire(eq("rate:login:127.0.0.1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void deve_bloquear_quando_contador_excede_limite() {
        when(valueOperations.increment("rate:refresh:10.0.0.1")).thenReturn(11L);
        when(redisTemplate.getExpire("rate:refresh:10.0.0.1", TimeUnit.SECONDS)).thenReturn(42L);

        RateLimitDecision decision = service.check("10.0.0.1", new RateLimitPolicy(10, "refresh"));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.limit()).isEqualTo(10);
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isEqualTo(42L);
        assertThat(decision.source()).isEqualTo("redis");
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }
}

