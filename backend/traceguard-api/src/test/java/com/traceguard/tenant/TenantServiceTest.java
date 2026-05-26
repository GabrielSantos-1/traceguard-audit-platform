package com.traceguard.tenant;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.tenant.dto.CreateTenantRequest;
import com.traceguard.tenant.dto.TenantResponse;
import com.traceguard.tenant.dto.UpdateTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantServiceTest {

    @Test
    void createsTenantAndRegistersAdminAudit() {
        TenantRepository repository = mock(TenantRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        TenantService service = new TenantService(repository, adminAuditService, new InputSanitizer());

        UUID actorId = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(actorId, UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        CreateTenantRequest request = new CreateTenantRequest("Tenant A", "tenant-a", TenantStatus.ACTIVE);

        when(repository.findBySlug("tenant-a")).thenReturn(Optional.empty());
        when(repository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0, Tenant.class);
            ReflectionTestUtils.setField(tenant, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(tenant, "createdAt", Instant.now());
            ReflectionTestUtils.setField(tenant, "updatedAt", Instant.now());
            return tenant;
        });

        TenantResponse response = service.create(request, actor, "corr-tenant-create");

        assertNotNull(response.id());
        assertEquals("tenant-a", response.slug());
        assertEquals(TenantStatus.ACTIVE, response.status());
        verify(adminAuditService, times(1))
                .record(eq(actorId), any(), eq("TENANT_CREATED"), eq("TENANT"), any(), eq("corr-tenant-create"), anyMap());
    }

    @Test
    void updatesTenantAndRegistersAdminAudit() {
        TenantRepository repository = mock(TenantRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        TenantService service = new TenantService(repository, adminAuditService, new InputSanitizer());

        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        tenant.setName("Tenant A");
        tenant.setSlug("tenant-a");
        tenant.setStatus(TenantStatus.ACTIVE);
        ReflectionTestUtils.setField(tenant, "createdAt", Instant.now().minusSeconds(60));
        ReflectionTestUtils.setField(tenant, "updatedAt", Instant.now().minusSeconds(60));

        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(repository.existsBySlugAndIdNot("tenant-b", tenantId)).thenReturn(false);
        when(repository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0, Tenant.class));

        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        UpdateTenantRequest request = new UpdateTenantRequest("Tenant B", "tenant-b", TenantStatus.SUSPENDED);

        TenantResponse response = service.update(tenantId, request, actor, "corr-tenant-update");
        assertEquals("Tenant B", response.name());
        assertEquals("tenant-b", response.slug());
        assertEquals(TenantStatus.SUSPENDED, response.status());
        verify(adminAuditService, times(1))
                .record(eq(actor.userId()), eq(tenantId), eq("TENANT_UPDATED"), eq("TENANT"), any(), eq("corr-tenant-update"), anyMap());
    }
}
