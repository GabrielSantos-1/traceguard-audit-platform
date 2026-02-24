package com.traceguard.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuditEventRequest(

        @NotBlank @Size(max = 100)
        String tenantId,

        @Size(max = 100)
        String userId,

        @NotBlank @Size(max = 100)
        String action,

        @Size(max = 100)
        String entity,

        @Size(max = 100)
        String entityId,

        @NotBlank @Size(max = 50)
        String severity,

        String metadataJson,
        String beforeJson,
        String afterJson
) {}
