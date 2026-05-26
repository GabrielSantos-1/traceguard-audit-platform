package com.traceguard.tenant.dto;

import com.traceguard.tenant.TenantStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 150) String name,
        @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        TenantStatus status
) {
}
