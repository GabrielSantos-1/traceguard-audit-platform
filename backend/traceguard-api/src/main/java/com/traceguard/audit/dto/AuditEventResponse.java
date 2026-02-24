package com.traceguard.audit.dto;

import java.time.Instant;

public record AuditEventResponse(
        String id,
        String tenantId,
        String userId,
        String action,
        String entity,
        String entityId,
        String severity,
        String ip,
        String userAgent,
        Instant occurredAt
) {}
