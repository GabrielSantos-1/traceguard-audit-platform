package com.traceguard.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentEventItem(
        UUID id,
        String action,
        String description,
        String severity,
        String ip,
        Instant occurredAt
) {}
