package com.traceguard.security;

import java.util.Set;
import java.util.UUID;

public record ApiKeyAuth(UUID tenantId, Set<ApiKeyScope> scopes) {
}
