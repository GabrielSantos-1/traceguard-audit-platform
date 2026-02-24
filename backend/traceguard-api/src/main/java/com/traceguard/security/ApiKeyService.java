package com.traceguard.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiKeyService {

    private final TenantApiKeyRepository repo;

    public ApiKeyService(TenantApiKeyRepository repo) {
        this.repo = repo;
    }

    public ApiKeyAuth resolveAuth(String rawKey) {
        String providedHash = ApiKeyHasher.sha256Hex(rawKey);
        Instant now = Instant.now();

        return repo.findByEnabledTrueAndRevokedAtIsNull()
                .stream()
                .filter(k -> k.getExpiresAt() == null || k.getExpiresAt().isAfter(now))
                .filter(k -> constantTimeEqualsHex(k.getKeyHash(), providedHash))
                .map(k -> new ApiKeyAuth(k.getTenantId(), Set.copyOf(k.getScopes())))
                .findFirst()
                .orElse(null);
    }

    public UUID resolveTenantId(String rawKey) {
        ApiKeyAuth auth = resolveAuth(rawKey);
        return auth != null ? auth.tenantId() : null;
    }

    private boolean constantTimeEqualsHex(String storedHash, String providedHash) {
        if (storedHash == null || providedHash == null) {
            return false;
        }

        byte[] left = storedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] right = providedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(left, right);
    }
}
