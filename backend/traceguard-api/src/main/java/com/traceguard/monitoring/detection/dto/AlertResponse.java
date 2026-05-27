package com.traceguard.monitoring.detection.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String ruleName,
        String severity,
        String title,
        String description,
        UUID tenantId,
        UUID userId,
        String sourceIp,
        JsonNode evidence,
        boolean acknowledged,
        UUID acknowledgedBy,
        Instant acknowledgedAt,
        Instant createdAt
) {
}
