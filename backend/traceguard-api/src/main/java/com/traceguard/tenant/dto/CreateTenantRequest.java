package com.traceguard.tenant.dto;

import com.traceguard.tenant.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,80}$") String slug,
        TenantStatus status
) {
}
