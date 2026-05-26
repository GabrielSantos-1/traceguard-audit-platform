package com.traceguard.apikey.dto;

import java.time.Instant;

public record RevokeApiKeyResponse(
        String id,
        Instant revokedAt,
        boolean enabled
) {
}
