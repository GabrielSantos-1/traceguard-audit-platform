package com.traceguard.apikey.dto;

import com.traceguard.security.ApiKeyScope;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateApiKeyRequest(
        UUID tenantId,
        @NotBlank @Size(max = 100) String name,
        @NotEmpty Set<ApiKeyScope> scopes,
        @Future Instant expiresAt
) {
}
