package com.traceguard.security;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.AuthController;
import com.traceguard.auth.AuthService;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.auth.JwtTokenService;
import com.traceguard.auth.RefreshTokenService;
import com.traceguard.auth.dto.LoginResponse;
import com.traceguard.adminaudit.AdminAuditController;
import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.adminaudit.dto.AdminAuditEventResponse;
import com.traceguard.monitoring.activity.UserActivityService;
import com.traceguard.monitoring.detection.BlockedIpRepository;
import com.traceguard.common.api.ApiExceptionHandler;
import com.traceguard.ratelimit.InMemoryRateLimiterService;
import com.traceguard.ratelimit.RateLimitDecision;
import com.traceguard.tenant.TenantAdminController;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { AuthController.class, TenantAdminController.class, AdminAuditController.class })
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
@TestPropertySource(properties = {
        "traceguard.rate-limit.login-per-minute=20",
        "traceguard.rate-limit.refresh-per-minute=20",
        "traceguard.rate-limit.audit-ingest-per-minute=100",
        "traceguard.rate-limit.audit-query-per-minute=100",
        "traceguard.rate-limit.admin-read-per-minute=100",
        "traceguard.rate-limit.admin-write-per-minute=100"
})
class SecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private AdminAuditService adminAuditService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private InMemoryRateLimiterService rateLimiter;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private UserActivityService userActivityService;

    @MockBean
    private BlockedIpRepository blockedIpRepository;

    @BeforeEach
    void setUp() {
        Counter counter = org.mockito.Mockito.mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        doReturn(counter).when(meterRegistry).counter(anyString());
        when(rateLimiter.check(anyString(), any()))
                .thenReturn(new RateLimitDecision(true, 100, 99, 0));
        when(authService.login(any(), any(), any(), any())).thenReturn(new LoginResponse(
                "access-token",
                "refresh-token",
                "Bearer",
                900,
                604800,
                new LoginResponse.UserSummary(UUID.randomUUID(), UUID.randomUUID(), "SUPER_ADMIN")));
        when(refreshTokenService.refresh(anyString(), any(), any(), any()))
                .thenReturn(new com.traceguard.auth.dto.RefreshTokenResponse(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        900,
                        604800));
        doNothing().when(refreshTokenService).logout(anyString(), any());
        when(adminAuditService.list(any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(
                List.of(new AdminAuditEventResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "ADMIN_LOGIN_SUCCESS",
                        "USER_ACCOUNT",
                        "user-1",
                        "corr-1",
                        Instant.now(),
                        null)),
                org.springframework.data.domain.PageRequest.of(0, 20),
                1
        ));
    }

    @Test
    void authEndpointsRemainPublicAndDoNotRequireJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@traceguard.local",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void protectedAdminRouteReturns401WhenJwtIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tenants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedAdminRouteReturns401WhenJwtIsInvalid() throws Exception {
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("invalid-token"), anyString()))
                .thenThrow(new RuntimeException("invalid"));

        mockMvc.perform(get("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedAdminRouteReturns401WhenJwtIsExpired() throws Exception {
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("expired-token"), anyString()))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid or expired token"));

        mockMvc.perform(get("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedAdminRouteReturns403ForRoleWithoutPermission() throws Exception {
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("operator-token"), anyString()))
                .thenReturn(new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.OPERATOR));

        mockMvc.perform(get("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void adminAuditRouteRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminAuditRouteRejectsUnauthorizedRole() throws Exception {
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("operator-audit-token"), anyString()))
                .thenReturn(new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.OPERATOR));

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", "Bearer operator-audit-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void adminAuditRouteAllowsTenantAdmin() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("tenant-admin-token"), anyString()))
                .thenReturn(new JwtPrincipal(userId, tenantId, AdminRole.TENANT_ADMIN));
        when(tenantRepository.findById(tenantId)).thenAnswer(invocation -> {
            java.util.UUID requestedTenantId = invocation.getArgument(0);
            if (tenantId.equals(requestedTenantId)) {
                com.traceguard.tenant.Tenant tenant = new com.traceguard.tenant.Tenant();
                tenant.setName("Tenant Admin");
                tenant.setSlug("tenant-admin");
                tenant.setStatus(com.traceguard.tenant.TenantStatus.ACTIVE);
                return java.util.Optional.of(tenant);
            }
            return java.util.Optional.empty();
        });

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", "Bearer tenant-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminAuditRouteAllowsSuperAdmin() throws Exception {
        when(jwtTokenService.parse(org.mockito.ArgumentMatchers.eq("super-admin-token"), anyString()))
                .thenReturn(new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN));

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .header("Authorization", "Bearer super-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
