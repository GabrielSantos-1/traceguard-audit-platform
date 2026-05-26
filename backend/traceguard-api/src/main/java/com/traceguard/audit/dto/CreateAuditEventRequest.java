package com.traceguard.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAuditEventRequest(

        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "userId must be a valid UUID"
        )
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

        @Size(max = 10000)
        String metadataJson,

        @Size(max = 10000)
        String beforeJson,

        @Size(max = 10000)
        String afterJson
) {}
