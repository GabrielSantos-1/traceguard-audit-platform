package com.traceguard.apikey.dto;

import com.traceguard.security.ApiKeyScope;

import java.time.Instant;
import java.util.Set;

public record ApiKeyListItemResponse(
        String id,
        String tenantId,
        String name,
        String keyPrefix,
        Set<ApiKeyScope> scopes,
        boolean enabled,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt,
        Instant createdAt
) {
}
