package com.traceguard.adminaudit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.adminaudit.dto.AdminAuditEventResponse;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditControllerTest {

    private MockMvc mockMvc;
    private AdminAuditService adminAuditService;
    private Authentication tenantAdminAuth;

    @BeforeEach
    void setUp() {
        adminAuditService = mock(AdminAuditService.class);
        AdminAuditController controller = new AdminAuditController(adminAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        tenantAdminAuth = new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.TENANT_ADMIN),
                null,
                List.of());
    }

    @Test
    void listReturnsPagedResponse() throws Exception {
        AdminAuditEventResponse event = new AdminAuditEventResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ADMIN_LOGIN_SUCCESS",
                "USER_ACCOUNT",
                "user-1",
                "corr-123",
                Instant.parse("2026-04-15T00:00:00Z"),
                new ObjectMapper().valueToTree(Map.of("role", "TENANT_ADMIN")));

        when(adminAuditService.list(any(), eq((JwtPrincipal) tenantAdminAuth.getPrincipal())))
                .thenReturn(new PageImpl<>(
                        List.of(event),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt")),
                        1));

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .principal(tenantAdminAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(event.id().toString()))
                .andExpect(jsonPath("$.data[0].action").value("ADMIN_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.totalElements").value(1))
                .andExpect(jsonPath("$.meta.correlationId").isNotEmpty());
    }

    @Test
    void listWithInvalidSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .principal(tenantAdminAuth)
                        .param("sort", "dropTable"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void listPassesFiltersAndPaginationToService() throws Exception {
        when(adminAuditService.list(any(), eq((JwtPrincipal) tenantAdminAuth.getPrincipal())))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/api/v1/admin/audit-events")
                        .principal(tenantAdminAuth)
                        .param("action", "ADMIN_LOGIN_SUCCESS")
                        .param("resourceType", "USER_ACCOUNT")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "-occurredAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(10));

        verify(adminAuditService).list(any(), eq((JwtPrincipal) tenantAdminAuth.getPrincipal()));
    }
}
