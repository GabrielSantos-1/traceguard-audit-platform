package com.traceguard.apikey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.apikey.dto.ApiKeyListItemResponse;
import com.traceguard.apikey.dto.CreateApiKeyRequest;
import com.traceguard.apikey.dto.CreateApiKeyResponse;
import com.traceguard.apikey.dto.RevokeApiKeyResponse;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.security.ApiKeyScope;
import com.traceguard.security.TenantApiKey;
import com.traceguard.security.TenantApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyAdminServiceTest {

    @Test
    void createReturnsRawKeyOnlyAtCreationAndListDoesNotExposeRawKey() throws Exception {
        TenantApiKeyRepository repository = mock(TenantApiKeyRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ApiKeyAdminService service = new ApiKeyAdminService(repository, adminAuditService, new InputSanitizer());

        UUID actorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        JwtPrincipal actor = new JwtPrincipal(actorId, UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        CreateApiKeyRequest request = new CreateApiKeyRequest(
                tenantId,
                "integration-main",
                Set.of(ApiKeyScope.AUDIT_READ, ApiKeyScope.AUDIT_WRITE),
                Instant.now().plusSeconds(3600)
        );

        when(repository.save(any(TenantApiKey.class))).thenAnswer(invocation -> {
            TenantApiKey entity = invocation.getArgument(0, TenantApiKey.class);
            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(entity, "createdAt", Instant.now());
            return entity;
        });

        CreateApiKeyResponse created = service.create(request, actor, "corr-create");
        assertNotNull(created.apiKey());
        assertTrue(created.apiKey().startsWith("tgk_"));
        assertNotEquals(created.apiKey(), created.keyPrefix());

        TenantApiKey persisted = new TenantApiKey();
        ReflectionTestUtils.setField(persisted, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(persisted, "tenantId", tenantId);
        ReflectionTestUtils.setField(persisted, "name", "integration-main");
        ReflectionTestUtils.setField(persisted, "keyPrefix", "0123456789ab");
        ReflectionTestUtils.setField(persisted, "scopes", Set.of(ApiKeyScope.AUDIT_READ));
        ReflectionTestUtils.setField(persisted, "enabled", true);
        ReflectionTestUtils.setField(persisted, "createdAt", Instant.now());

        when(repository.findByTenantId(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(persisted)));

        ApiKeyListItemResponse listItem = service.list(tenantId, 0, 10, actor).getContent().getFirst();
        String serialized = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(listItem);
        assertFalse(serialized.contains("rawKey"));

        verify(adminAuditService, times(1))
                .record(eq(actorId), eq(tenantId), eq("API_KEY_CREATED"), eq("API_KEY"), any(), eq("corr-create"), anyMap());
    }

    @Test
    void revokeMarksApiKeyAsDisabledAndRevoked() {
        TenantApiKeyRepository repository = mock(TenantApiKeyRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        ApiKeyAdminService service = new ApiKeyAdminService(repository, adminAuditService, new InputSanitizer());

        UUID keyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantApiKey key = new TenantApiKey();
        ReflectionTestUtils.setField(key, "id", keyId);
        ReflectionTestUtils.setField(key, "tenantId", tenantId);
        ReflectionTestUtils.setField(key, "name", "to-revoke");
        ReflectionTestUtils.setField(key, "keyPrefix", "aaaa1111bbbb");
        ReflectionTestUtils.setField(key, "enabled", true);
        when(repository.findById(keyId)).thenReturn(Optional.of(key));
        when(repository.save(any(TenantApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0, TenantApiKey.class));

        JwtPrincipal actor = new JwtPrincipal(UUID.randomUUID(), tenantId, AdminRole.TENANT_ADMIN);
        RevokeApiKeyResponse response = service.revoke(keyId, actor, "corr-revoke");

        assertFalse(response.enabled());
        assertNotNull(response.revokedAt());
    }
}
