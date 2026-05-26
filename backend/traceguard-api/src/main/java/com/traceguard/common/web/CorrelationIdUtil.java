package com.traceguard.common.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class CorrelationIdUtil {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String ATTRIBUTE_NAME = "traceguard.correlationId";
    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    private CorrelationIdUtil() {
    }

    public static String getOrCreate(HttpServletRequest request) {
        Object attribute = request.getAttribute(ATTRIBUTE_NAME);
        if (attribute instanceof String existing && !existing.isBlank()) {
            return existing;
        }

        String headerValue = request.getHeader(HEADER_NAME);
        String correlationId;
        if (headerValue != null && !headerValue.isBlank() && headerValue.length() <= MAX_CORRELATION_ID_LENGTH) {
            correlationId = headerValue.trim();
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        request.setAttribute(ATTRIBUTE_NAME, correlationId);
        return correlationId;
    }
}
