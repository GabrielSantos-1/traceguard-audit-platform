package com.traceguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security headers to every HTTP response.
 * Applied early in the filter chain to ensure all responses include them.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP = "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; "
            + "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://cdn.tailwindcss.com https://cdn.jsdelivr.net; "
            + "font-src 'self' data: https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; "
            + "img-src 'self' data: blob:; "
            + "connect-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("X-Frame-Options", "DENY");
        // Remove Server header (set by container)
        response.setHeader("Server", "");
        chain.doFilter(request, response);
    }
}
