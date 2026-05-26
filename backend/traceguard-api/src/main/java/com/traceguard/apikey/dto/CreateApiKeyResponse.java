package com.traceguard.apikey.dto;

import com.traceguard.security.ApiKeyScope;

import java.time.Instant;
import java.util.Set;

public record CreateApiKeyResponse(
        String id,
        String tenantId,
        String name,
        Set<ApiKeyScope> scopes,
        String keyPrefix,
        String apiKey,
        Instant expiresAt,
        Instant createdAt
) {
}
