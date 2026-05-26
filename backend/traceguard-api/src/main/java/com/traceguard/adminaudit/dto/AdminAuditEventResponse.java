package com.traceguard.adminaudit.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditEventResponse(
        UUID id,
        UUID adminId,
        UUID tenantId,
        String action,
        String resourceType,
        String resourceId,
        String correlationId,
        Instant occurredAt,
        JsonNode detailsJson
) {
}
