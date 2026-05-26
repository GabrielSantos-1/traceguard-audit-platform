package com.traceguard.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.audit.dto.CreateAuditEventRequest;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.security.ApiKeyScope;
import com.traceguard.security.TenantContext;
import com.traceguard.tenant.Tenant;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditEventServiceTenantIsolationTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void createUsesTenantFromContextAndNeverFromClientPayload() {
        AuditEventRepository repo = mock(AuditEventRepository.class);
        when(repo.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0, AuditEvent.class));
        TenantRepository tenantRepository = mock(TenantRepository.class);

        UUID tenantFromContext = UUID.randomUUID();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(tenantRepository.findById(tenantFromContext)).thenReturn(java.util.Optional.of(tenant));

        AuditEventService service = new AuditEventService(
                repo,
                new ObjectMapper(),
                new InputSanitizer(),
                new SimpleMeterRegistry(),
                tenantRepository
        );

        TenantContext.setTenantId(tenantFromContext);
        TenantContext.setScopes(Set.of(ApiKeyScope.AUDIT_WRITE));

        CreateAuditEventRequest request = new CreateAuditEventRequest(
                null,
                "create-event",
                "audit",
                "evt-1",
                "INFO",
                "{\"tenantId\":\"" + UUID.randomUUID() + "\"}",
                null,
                null
        );

        service.create(request, "127.0.0.1", "JUnit");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        org.mockito.Mockito.verify(repo).save(captor.capture());
        AuditEvent persisted = captor.getValue();

        assertEquals(tenantFromContext, persisted.getTenantId());
    }

    @Test
    void listRejectsSortFieldOutsideAllowlist() {
        AuditEventRepository repo = mock(AuditEventRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = mock(Tenant.class);
        when(tenant.getStatus()).thenReturn(TenantStatus.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(java.util.Optional.of(tenant));

        AuditEventService service = new AuditEventService(
                repo,
                new ObjectMapper(),
                new InputSanitizer(),
                new SimpleMeterRegistry(),
                tenantRepository
        );

        TenantContext.setTenantId(tenantId);
        TenantContext.setScopes(Set.of(ApiKeyScope.AUDIT_READ));

        assertThrows(
                ResponseStatusException.class,
                () -> service.list(
                        null,
                        null,
                        null,
                        Instant.now().minusSeconds(3600),
                        Instant.now(),
                        0,
                        20,
                        "tenantId",
                        "desc"
                )
        );
    }
}
