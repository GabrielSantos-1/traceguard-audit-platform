package com.traceguard.common.api;

public record ApiMeta(
        String correlationId,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean hasNext,
        Boolean hasPrevious
) {
    public static ApiMeta basic(String correlationId) {
        return new ApiMeta(correlationId, null, null, null, null, null, null);
    }

    public static ApiMeta paged(
            String correlationId,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        return new ApiMeta(correlationId, page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
}
