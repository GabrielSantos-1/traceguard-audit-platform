package com.traceguard.auth;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        UUID tenantId,
        AdminRole role
) {
}
