package com.traceguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyFilterTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void clearsTenantContextWhenApiKeyIsMissing() throws ServletException, IOException {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService);

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
        ApiKeyFilter filter = new ApiKeyFilter(apiKeyService);
        UUID tenantId = UUID.randomUUID();

        when(apiKeyService.resolveAuth("valid-key"))
                .thenReturn(new ApiKeyAuth(tenantId, java.util.Set.of(ApiKeyScope.AUDIT_READ)));

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
}
