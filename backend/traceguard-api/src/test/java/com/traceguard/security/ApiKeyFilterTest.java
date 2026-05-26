package com.traceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyFilterTest {

    private ObjectMapper newTestMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearsTenantContextWhenApiKeyIsMissing() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        TenantContext.setTenantId(UUID.randomUUID());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 401);
        assertTrue(!chainCalled.get());
        assertNull(TenantContext.getTenantId());
        assertTrue(TenantContext.getScopes().isEmpty());
    }

    @Test
    void clearsTenantContextWhenDownstreamThrows() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());
        UUID tenantId = UUID.randomUUID();

        when(apiKeyService.resolveAuth("valid-key"))
                .thenReturn(new ApiKeyAuth(tenantId, java.util.Set.of(ApiKeyScope.AUDIT_READ)));
        when(apiKeyService.resolveAuthDetailed(anyString(), anyString()))
                .thenReturn(ApiKeyValidationResult.success(new ApiKeyAuth(tenantId, java.util.Set.of(ApiKeyScope.AUDIT_READ))));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader("X-API-Key", "valid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        assertNull(TenantContext.getTenantId());
        assertTrue(TenantContext.getScopes().isEmpty());
    }

    @Test
    void returnsForbiddenWhenTenantIsSuspended() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        when(apiKeyService.resolveAuthDetailed(anyString(), anyString()))
                .thenReturn(ApiKeyValidationResult.failure(ApiKeyValidationFailureReason.TENANT_SUSPENDED));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader("X-API-Key", "suspended-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 403);
        assertTrue(!chainCalled.get());
    }

    @Test
    void returnsUnauthorizedWhenApiKeyIsInvalid() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        when(apiKeyService.resolveAuthDetailed(anyString(), anyString()))
                .thenReturn(ApiKeyValidationResult.failure(ApiKeyValidationFailureReason.INVALID));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader("X-API-Key", "invalid-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 401);
        assertTrue(!chainCalled.get());
    }

    @Test
    void returnsUnauthorizedWhenApiKeyIsExpired() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        when(apiKeyService.resolveAuthDetailed(anyString(), anyString()))
                .thenReturn(ApiKeyValidationResult.failure(ApiKeyValidationFailureReason.EXPIRED));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader("X-API-Key", "expired-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 401);
        assertTrue(!chainCalled.get());
    }

    @Test
    void returnsUnauthorizedWhenApiKeyIsRevoked() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        when(apiKeyService.resolveAuthDetailed(anyString(), anyString()))
                .thenReturn(ApiKeyValidationResult.failure(ApiKeyValidationFailureReason.REVOKED));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader("X-API-Key", "revoked-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 401);
        assertTrue(!chainCalled.get());
    }

    @Test
    void skipsApiKeyValidationForPublicHealthEndpoint() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService, newTestMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setServletPath("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertTrue(response.getStatus() == 200);
        assertNull(TenantContext.getTenantId());
    }
}
