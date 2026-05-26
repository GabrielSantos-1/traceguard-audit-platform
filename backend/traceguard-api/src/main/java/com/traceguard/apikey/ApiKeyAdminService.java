package com.traceguard.apikey;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.apikey.dto.ApiKeyListItemResponse;
import com.traceguard.apikey.dto.CreateApiKeyRequest;
import com.traceguard.apikey.dto.CreateApiKeyResponse;
import com.traceguard.apikey.dto.RevokeApiKeyResponse;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.security.ApiKeyHasher;
import com.traceguard.security.TenantApiKey;
import com.traceguard.security.TenantApiKeyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiKeyAdminService {

    private final TenantApiKeyRepository repository;
    private final AdminAuditService adminAuditService;
    private final InputSanitizer sanitizer;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyAdminService(
            TenantApiKeyRepository repository,
            AdminAuditService adminAuditService,
            InputSanitizer sanitizer
    ) {
        this.repository = repository;
        this.adminAuditService = adminAuditService;
        this.sanitizer = sanitizer;
    }

    public CreateApiKeyResponse create(CreateApiKeyRequest request, JwtPrincipal actor, String correlationId) {
        UUID tenantId = resolveTargetTenant(actor, request.tenantId());
        String rawKey = generateRawApiKey();
        String hash = ApiKeyHasher.sha256Hex(rawKey);
        String prefix = hash.substring(0, 12);

        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setName(sanitizer.sanitizeText(request.name(), 100, "name"));
        apiKey.setScopes(Set.copyOf(request.scopes()));
        apiKey.setKeyHash(hash);
        apiKey.setKeyPrefix(prefix);
        apiKey.setEnabled(true);
        apiKey.setExpiresAt(request.expiresAt());
        TenantApiKey saved = repository.save(apiKey);

                adminAuditService.record(
                        actor.userId(),
                        tenantId,
                        "API_KEY_CREATED",
                        "API_KEY",
                        saved.getId().toString(),
                        correlationId,
                        Map.of("scopes", saved.getScopes() != null ? saved.getScopes().toString() : "[]")
                );

        return new CreateApiKeyResponse(
                saved.getId().toString(),
                saved.getTenantId().toString(),
                saved.getName(),
                saved.getScopes(),
                saved.getKeyPrefix(),
                rawKey,
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }

    public Page<ApiKeyListItemResponse> list(UUID tenantIdQuery, int page, int size, JwtPrincipal actor) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        UUID tenantId = resolveListTenant(actor, tenantIdQuery);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        if (tenantId == null) {
            return repository.findAll(pageable).map(this::toListResponse);
        }
        return repository.findByTenantId(tenantId, pageable).map(this::toListResponse);
    }

    public RevokeApiKeyResponse revoke(UUID apiKeyId, JwtPrincipal actor, String correlationId) {
        TenantApiKey apiKey = repository.findById(apiKeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));

        if (actor.role() != AdminRole.SUPER_ADMIN && !apiKey.getTenantId().equals(actor.tenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot revoke API key from another tenant");
        }

        apiKey.setEnabled(false);
        apiKey.setRevokedAt(Instant.now());
        TenantApiKey saved = repository.save(apiKey);

        adminAuditService.record(
                actor.userId(),
                saved.getTenantId(),
                "API_KEY_REVOKED",
                "API_KEY",
                saved.getId().toString(),
                correlationId,
                Map.of("name", saved.getName(), "prefix", saved.getKeyPrefix())
        );

        return new RevokeApiKeyResponse(saved.getId().toString(), saved.getRevokedAt(), saved.isEnabled());
    }

    private ApiKeyListItemResponse toListResponse(TenantApiKey key) {
        return new ApiKeyListItemResponse(
                key.getId().toString(),
                key.getTenantId().toString(),
                key.getName(),
                key.getKeyPrefix(),
                key.getScopes(),
                key.isEnabled(),
                key.getExpiresAt(),
                key.getLastUsedAt(),
                key.getRevokedAt(),
                key.getCreatedAt()
        );
    }

    private UUID resolveTargetTenant(JwtPrincipal actor, UUID requestedTenantId) {
        if (actor.role() == AdminRole.SUPER_ADMIN) {
            if (requestedTenantId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required for SUPER_ADMIN");
            }
            return requestedTenantId;
        }
        if (actor.role() != AdminRole.TENANT_ADMIN && actor.role() != AdminRole.OPERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role cannot create API keys");
        }
        return actor.tenantId();
    }

    private UUID resolveListTenant(JwtPrincipal actor, UUID requestedTenantId) {
        if (actor.role() == AdminRole.SUPER_ADMIN) {
            return requestedTenantId;
        }
        return actor.tenantId();
    }

    private String generateRawApiKey() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return "tgk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
