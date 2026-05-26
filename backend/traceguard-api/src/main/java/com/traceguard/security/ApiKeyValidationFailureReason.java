package com.traceguard.security;

public enum ApiKeyValidationFailureReason {
    INVALID,
    EXPIRED,
    REVOKED,
    DISABLED,
    TENANT_SUSPENDED
}

