package com.traceguard.security;

import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final TenantApiKeyRepository repo;
    private final TenantRepository tenantRepository;
    private final MeterRegistry meterRegistry;

    public ApiKeyService(TenantApiKeyRepository repo, TenantRepository tenantRepository, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.tenantRepository = tenantRepository;
        this.meterRegistry = meterRegistry;
    }

    public ApiKeyAuth resolveAuth(String rawKey) {
        ApiKeyValidationResult result = resolveAuthDetailed(rawKey, null);
        return result.isAuthenticated() ? result.auth() : null;
    }

    public ApiKeyValidationResult resolveAuthDetailed(String rawKey, String correlationId) {
        if (rawKey == null || rawKey.isBlank() || rawKey.length() > 512) {
            return fail(ApiKeyValidationFailureReason.INVALID, null, correlationId);
        }

        String providedHash = ApiKeyHasher.sha256Hex(rawKey);
        Instant now = Instant.now();

        TenantApiKey key = repo.findByKeyHash(providedHash).orElse(null);
        if (key == null) {
            return fail(ApiKeyValidationFailureReason.INVALID, null, correlationId);
        }
        if (!key.isEnabled()) {
            return fail(ApiKeyValidationFailureReason.DISABLED, key.getTenantId(), correlationId);
        }
        if (key.getRevokedAt() != null) {
            return fail(ApiKeyValidationFailureReason.REVOKED, key.getTenantId(), correlationId);
        }
        if (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(now)) {
            return fail(ApiKeyValidationFailureReason.EXPIRED, key.getTenantId(), correlationId);
        }

        boolean tenantActive = tenantRepository.findById(key.getTenantId())
                .map(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
        if (!tenantActive) {
            meterRegistry.counter("traceguard.tenant.blocked", "source", "apikey").increment();
            log.warn(
                    "event=TENANT_SUSPENDED outcome=failure reasonCode=tenant_suspended source=apikey tenantId={} correlationId={}",
                    key.getTenantId(),
                    correlationId == null ? "n/a" : correlationId);
            return ApiKeyValidationResult.failure(ApiKeyValidationFailureReason.TENANT_SUSPENDED);
        }

        key.setLastUsedAt(now);
        repo.save(key);
        return ApiKeyValidationResult.success(new ApiKeyAuth(key.getTenantId(), Set.copyOf(key.getScopes())));
    }

    public UUID resolveTenantId(String rawKey) {
        ApiKeyAuth auth = resolveAuth(rawKey);
        return auth != null ? auth.tenantId() : null;
    }

    private ApiKeyValidationResult fail(ApiKeyValidationFailureReason reason, UUID tenantId, String correlationId) {
        switch (reason) {
            case EXPIRED -> meterRegistry.counter("traceguard.apikey.expired").increment();
            case REVOKED -> meterRegistry.counter("traceguard.apikey.revoked").increment();
            default -> meterRegistry.counter("traceguard.apikey.invalid").increment();
        }

        String event = switch (reason) {
            case EXPIRED -> "API_KEY_EXPIRED";
            case REVOKED -> "API_KEY_REVOKED";
            case DISABLED, INVALID -> "API_KEY_INVALID";
            case TENANT_SUSPENDED -> "TENANT_SUSPENDED";
        };

        log.warn(
                "event={} outcome=failure reasonCode={} tenantId={} correlationId={}",
                event,
                reason.name().toLowerCase(),
                tenantId == null ? "n/a" : tenantId,
                correlationId == null ? "n/a" : correlationId);
        return ApiKeyValidationResult.failure(reason);
    }
}
