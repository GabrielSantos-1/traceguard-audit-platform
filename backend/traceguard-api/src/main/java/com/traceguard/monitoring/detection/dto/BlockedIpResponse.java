package com.traceguard.monitoring.detection.dto;

import java.time.Instant;
import java.util.UUID;

public record BlockedIpResponse(
        UUID id,
        String ip,
        UUID tenantId,
        String ruleName,
        String reason,
        Instant blockedAt,
        Instant expiresAt,
        boolean active
) {
}
