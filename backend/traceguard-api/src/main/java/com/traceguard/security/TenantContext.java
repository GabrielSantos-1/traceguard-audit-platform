package com.traceguard.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Set<ApiKeyScope>> SCOPES = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) { TENANT_ID.set(tenantId); }
    public static void setScopes(Set<ApiKeyScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            SCOPES.set(EnumSet.noneOf(ApiKeyScope.class));
            return;
        }
        SCOPES.set(EnumSet.copyOf(scopes));
    }
    public static UUID getTenantId() { return TENANT_ID.get(); }
    public static Set<ApiKeyScope> getScopes() {
        Set<ApiKeyScope> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(scopes);
    }
    public static boolean hasScope(ApiKeyScope scope) {
        return scope != null && getScopes().contains(scope);
    }
    public static void clear() {
        TENANT_ID.remove();
        SCOPES.remove();
    }
}
