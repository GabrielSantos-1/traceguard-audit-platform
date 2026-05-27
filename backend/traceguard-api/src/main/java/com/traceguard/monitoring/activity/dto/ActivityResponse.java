package com.traceguard.monitoring.activity.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userRole,
        String action,
        String resourceType,
        String resourceId,
        String httpMethod,
        String httpPath,
        int httpStatus,
        String ip,
        String correlationId,
        boolean success,
        JsonNode beforeSnapshot,
        JsonNode afterSnapshot,
        String diffSummary,
        Long durationMs,
        Instant occurredAt
) {
}
