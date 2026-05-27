package com.traceguard.monitoring.activity;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.monitoring.activity.dto.ActivityQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserActivityServiceTest {

    private UserActivityService service;
    private UserActivityRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(UserActivityRepository.class);
        service = new UserActivityService(repository, new InputSanitizer());
    }

    @Test
    void listRequiresAuthentication() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.list(new ActivityQueryRequest(), null));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void superAdminCanListAllTenants() {
        UUID actorTenant = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), actorTenant, AdminRole.SUPER_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();

        UserActivityEvent event = createTestEvent(actorTenant);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        Page<UserActivityService.ActivityRecord> result = null;
        Page<com.traceguard.monitoring.activity.dto.ActivityResponse> page = service.list(query, actor);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).httpPath()).isEqualTo("/api/v1/admin/test");
    }

    @Test
    void tenantAdminOnlySeesOwnTenant() {
        UUID actorTenant = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), actorTenant, AdminRole.TENANT_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();

        UserActivityEvent event = createTestEvent(actorTenant);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        Page<com.traceguard.monitoring.activity.dto.ActivityResponse> page = service.list(query, actor);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void filtersByUserIdWhenProvided() {
        UUID actorTenant = UUID.randomUUID();
        UUID targetUser = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), actorTenant, AdminRole.SUPER_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setUserId(targetUser.toString());

        UserActivityEvent event = createTestEvent(actorTenant);
        event.setUserId(targetUser);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        Page<com.traceguard.monitoring.activity.dto.ActivityResponse> page = service.list(query, actor);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).userId()).isEqualTo(targetUser);
    }

    @Test
    void sortOutsideAllowlistReturnsBadRequest() {
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setSort("-unknownField");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.list(query, actor));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidToBeforeFromReturnsBadRequest() {
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setFrom(Instant.now());
        query.setTo(Instant.now().minusSeconds(3600));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.list(query, actor));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidUserIdFormatReturnsBadRequest() {
        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setUserId("not-a-uuid");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.list(query, actor));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void recordAsyncDoesNotThrowOnSuccess() {
        when(repository.save(any(UserActivityEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordAsync(new UserActivityService.ActivityRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SUPER_ADMIN",
                "READ",
                "admin/tenants",
                null,
                "GET",
                "/api/v1/admin/tenants",
                200,
                "192.168.1.1",
                "TestAgent/1.0",
                "corr-1",
                true,
                null,
                null,
                null,
                42L,
                Instant.now()
        ));

        verify(repository, timeout(1000)).save(any(UserActivityEvent.class));
    }

    @Test
    void recordAsyncHandlesNullOptionalFields() {
        when(repository.save(any(UserActivityEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordAsync(new UserActivityService.ActivityRecord(
                UUID.randomUUID(),
                null,
                null,
                "ERROR",
                null,
                null,
                "GET",
                "/api/v1/test",
                500,
                null,
                null,
                "corr-2",
                false,
                null,
                null,
                null,
                null,
                Instant.now()
        ));

        verify(repository, timeout(1000)).save(any(UserActivityEvent.class));
    }

    private UserActivityEvent createTestEvent(UUID tenantId) {
        UserActivityEvent event = new UserActivityEvent();
        event.setId(UUID.randomUUID());
        event.setTenantId(tenantId);
        event.setUserId(UUID.randomUUID());
        event.setUserRole("TENANT_ADMIN");
        event.setAction("READ");
        event.setResourceType("admin/tenants");
        event.setHttpMethod("GET");
        event.setHttpPath("/api/v1/admin/test");
        event.setHttpStatus(200);
        event.setIp("127.0.0.1");
        event.setCorrelationId("corr-test");
        event.setSuccess(true);
        event.setDurationMs(15L);
        event.setOccurredAt(Instant.now());
        return event;
    }
}
