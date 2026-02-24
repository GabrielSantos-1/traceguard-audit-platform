package com.traceguard.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpResolver {
    private IpResolver(){}

    public static String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // primeiro IP é o do cliente
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
