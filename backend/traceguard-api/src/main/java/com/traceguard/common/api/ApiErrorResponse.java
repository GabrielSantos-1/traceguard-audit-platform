package com.traceguard.common.api;

public record ApiErrorResponse(
        boolean success,
        ApiErrorBody error
) {
    public static ApiErrorResponse of(ApiErrorBody error) {
        return new ApiErrorResponse(false, error);
    }
}
