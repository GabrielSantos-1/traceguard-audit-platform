package com.traceguard.adminaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.adminaudit.dto.AdminAuditQueryRequest;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuditServiceTest {

    private AdminAuditService service;
    private AdminAuditEventRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = mock(AdminAuditEventRepository.class);
        service = new AdminAuditService(
                repository,
                new AuditSanitizer(objectMapper),
                new InputSanitizer());
    }

    @Test
    void tenantAdminIgnoresExternalTenantFilter() {
        UUID actorTenant = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), actorTenant, AdminRole.TENANT_ADMIN);
        AdminAuditQueryRequest query = new AdminAuditQueryRequest();
        query.setTenantId(UUID.randomUUID().toString());

        AdminAuditQueryCriteria criteria = service.resolveCriteria(query, actor);

        assertThat(criteria.tenantId()).isEqualTo(actorTenant);
    }

    @Test
    void superAdminCanUseTenantFilter() {
        UUID requestedTenant = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        AdminAuditQueryRequest query = new AdminAuditQueryRequest();
        query.setTenantId(requestedTenant.toString());

        AdminAuditQueryCriteria criteria = service.resolveCriteria(query, actor);

        assertThat(criteria.tenantId()).isEqualTo(requestedTenant);
    }

    @Test
    void unsupportedRoleIsForbidden() {
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.OPERATOR);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveCriteria(new AdminAuditQueryRequest(), actor));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sortOutsideAllowlistReturnsBadRequest() {
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        AdminAuditQueryRequest query = new AdminAuditQueryRequest();
        query.setSort("-unknownField");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveCriteria(query, actor));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordSanitizesSensitiveFieldsBeforePersisting() {
        when(repository.save(any(AdminAuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ADMIN_TEST",
                "TEST_RESOURCE",
                "resource-1",
                "corr-1",
                Map.of(
                        "password", "raw-password",
                        "accessToken", "raw-token",
                        "apiKey", "raw-api-key",
                        "email", "admin@traceguard.local",
                        "ipAddress", "10.20.30.40"));

        ArgumentCaptor<AdminAuditEvent> captor = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(repository).save(captor.capture());
        JsonNode details = captor.getValue().getDetailsJson();

        assertThat(details.has("password")).isFalse();
        assertThat(details.has("accessToken")).isFalse();
        assertThat(details.has("apiKey")).isFalse();
        assertThat(details.get("email").asText()).isEqualTo("a***@traceguard.local");
        assertThat(details.get("ipAddress").asText()).isEqualTo("10.20.***.***");
    }

    @Test
    void listSanitizesResponseDetails() throws Exception {
        AdminAuditEvent event = new AdminAuditEvent();
        setPrivateField(event, "id", UUID.randomUUID());
        event.setAdminId(UUID.randomUUID());
        event.setTenantId(UUID.randomUUID());
        event.setAction("ADMIN_TEST");
        event.setResourceType("RESOURCE");
        event.setResourceId("id-1");
        event.setCorrelationId("corr-1");
        setPrivateField(event, "occurredAt", Instant.parse("2026-04-16T00:00:00Z"));
        event.setDetailsJson(objectMapper.valueToTree(Map.of(
                "token", "raw-token",
                "userAgent", "VeryLongUserAgent-".repeat(30),
                "email", "admin@traceguard.local")));

        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        AdminAuditQueryRequest query = new AdminAuditQueryRequest();
        var page = service.list(query, new JwtPrincipal(UUID.randomUUID(), event.getTenantId(), AdminRole.TENANT_ADMIN));

        JsonNode details = page.getContent().get(0).detailsJson();
        assertThat(details.has("token")).isFalse();
        assertThat(details.get("email").asText()).isEqualTo("a***@traceguard.local");
        assertThat(details.get("userAgent").asText().length()).isLessThanOrEqualTo(180);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
