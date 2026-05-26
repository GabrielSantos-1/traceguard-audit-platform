package com.traceguard.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.traceguard.security.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimiterService rateLimiter;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiterService.class);

        filter = new RateLimitFilter(
                rateLimiter,
                new ObjectMapper(),
                new SimpleMeterRegistry(),
                5, // login
                10, // refresh
                120, // audit ingest
                60, // audit query
                120, // admin read
                30 // admin write
        );
    }

    @Test
    void deve_aplicar_bucket_refresh_para_post_auth_refresh() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(rateLimiter.check(eq("127.0.0.1"), any(RateLimitPolicy.class)))
                .thenReturn(new RateLimitDecision(true, 10, 9, 0, "memory"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
        verify(rateLimiter).check(eq("127.0.0.1"), any(RateLimitPolicy.class));
    }

    @Test
    void deve_retornar_429_quando_refresh_exceder_limite() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(rateLimiter.check(eq("127.0.0.1"), any(RateLimitPolicy.class)))
                .thenReturn(new RateLimitDecision(false, 10, 0, 60, "memory"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        verify(rateLimiter).check(eq("127.0.0.1"), any(RateLimitPolicy.class));
    }

    @Test
    void nao_deve_limitar_rota_sem_policy() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void deve_usar_tenant_context_no_bucket_de_audit_sem_jwt() throws Exception {
        java.util.UUID tenantId = java.util.UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
            request.setRemoteAddr("127.0.0.1");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            when(rateLimiter.check(eq(tenantId.toString()), any(RateLimitPolicy.class)))
                    .thenReturn(new RateLimitDecision(true, 60, 59, 0, "memory"));

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(rateLimiter).check(eq(tenantId.toString()), any(RateLimitPolicy.class));
        } finally {
            TenantContext.clear();
        }
    }
}
