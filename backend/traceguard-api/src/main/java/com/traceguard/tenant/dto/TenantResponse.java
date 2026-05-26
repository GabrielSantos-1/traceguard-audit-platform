package com.traceguard.tenant.dto;

import com.traceguard.tenant.TenantStatus;

import java.time.Instant;

public record TenantResponse(
        String id,
        String name,
        String slug,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
