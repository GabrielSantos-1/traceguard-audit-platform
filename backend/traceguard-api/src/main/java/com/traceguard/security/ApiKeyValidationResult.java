package com.traceguard.security;

public record ApiKeyValidationResult(
        ApiKeyAuth auth,
        ApiKeyValidationFailureReason failureReason
) {
    public static ApiKeyValidationResult success(ApiKeyAuth auth) {
        return new ApiKeyValidationResult(auth, null);
    }

    public static ApiKeyValidationResult failure(ApiKeyValidationFailureReason reason) {
        return new ApiKeyValidationResult(null, reason);
    }

    public boolean isAuthenticated() {
        return auth != null;
    }
}

