package com.traceguard.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record CriticalAlertItem(
        UUID id,
        String title,
        String description,
        String severity,
        String ip,
        Instant occurredAt
) {}
