package com.traceguard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.traceguard.tenant.Tenant;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUnauthorizedWhenTokenIsInvalid() throws ServletException, IOException {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        when(tokenService.parse(eq("bad-token"), anyString())).thenThrow(new RuntimeException("bad-token"));

        JwtAuthFilter filter = new JwtAuthFilter(
                tokenService,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                tenantRepository,
                new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/tenants");
        request.setServletPath("/api/v1/admin/tenants");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 401);
        assertTrue(!chainCalled.get());
    }

    @Test
    void populatesSecurityContextWhenTokenIsValid() throws ServletException, IOException {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        Tenant tenant = new Tenant();
        tenant.setStatus(TenantStatus.ACTIVE);
        when(tokenService.parse(eq("good-token"), anyString())).thenReturn(
                new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN)
        );
        when(tenantRepository.findById(any())).thenReturn(java.util.Optional.of(tenant));

        JwtAuthFilter filter = new JwtAuthFilter(
                tokenService,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                tenantRepository,
                new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/tenants");
        request.setServletPath("/api/v1/admin/tenants");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertTrue(SecurityContextHolder.getContext().getAuthentication() != null);
    }

    @Test
    void returnsForbiddenWhenTenantIsSuspended() throws ServletException, IOException {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(tokenService.parse(eq("tenant-admin-token"), anyString())).thenReturn(
                new JwtPrincipal(userId, tenantId, AdminRole.TENANT_ADMIN)
        );
        Tenant tenant = new Tenant();
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findById(eq(tenantId))).thenReturn(java.util.Optional.of(tenant));

        JwtAuthFilter filter = new JwtAuthFilter(
                tokenService,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                tenantRepository,
                new SimpleMeterRegistry());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/tenants");
        request.setServletPath("/api/v1/admin/tenants");
        request.addHeader("Authorization", "Bearer tenant-admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(response.getStatus() == 403);
        assertTrue(!chainCalled.get());
    }
}
