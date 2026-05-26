package com.traceguard.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiErrorBody(
        String code,
        String message,
        Instant timestamp,
        String path,
        String correlationId,
        Map<String, String> details
) {
}
