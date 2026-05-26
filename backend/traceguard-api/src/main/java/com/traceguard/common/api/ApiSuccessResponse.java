package com.traceguard.common.api;

public record ApiSuccessResponse<T>(
        boolean success,
        T data,
        ApiMeta meta
) {
    public static <T> ApiSuccessResponse<T> of(T data, ApiMeta meta) {
        return new ApiSuccessResponse<>(true, data, meta);
    }
}
