package com.traceguard.auth;

public enum AdminRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    AUDITOR,
    OPERATOR;

    public static AdminRole parse(String raw) {
        return AdminRole.valueOf(raw.trim().toUpperCase());
    }
}
