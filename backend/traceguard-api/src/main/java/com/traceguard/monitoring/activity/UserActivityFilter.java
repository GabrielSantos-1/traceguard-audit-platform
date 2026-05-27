package com.traceguard.monitoring.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.util.IpResolver;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserActivityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserActivityFilter.class);
    private static final Set<String> SKIPPED_PATHS = Set.of(
            "/actuator/", "/swagger-ui", "/v3/api-docs", "/h2-console",
            "/index.html", "/favicon.ico", "/static/"
    );
    private static final Set<String> SKIPPED_EXTENSIONS = Set.of(".js", ".css", ".png", ".jpg", ".gif", ".ico", ".svg", ".woff", ".woff2");

    private final UserActivityService activityService;
    private final ObjectMapper objectMapper;

    public UserActivityFilter(UserActivityService activityService, ObjectMapper objectMapper) {
        this.activityService = activityService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }

        long startNanos = System.nanoTime();
        StatusCaptureWrapper wrappedResponse = new StatusCaptureWrapper(response);

        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String correlationId = CorrelationIdUtil.getOrCreate(request);
                String ip = IpResolver.resolve(request);
                String userAgent = request.getHeader("User-Agent");

                UUID tenantId = null;
                UUID userId = null;
                String userRole = null;

                if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                    tenantId = principal.tenantId();
                    userId = principal.userId();
                    userRole = principal.role() != null ? principal.role().name() : null;
                }

                String action = determineAction(request.getMethod(), wrappedResponse.getStatus());

                activityService.recordAsync(new UserActivityService.ActivityRecord(
                        tenantId,
                        userId,
                        userRole,
                        action,
                        guessResourceType(path),
                        null,
                        request.getMethod(),
                        path,
                        wrappedResponse.getStatus(),
                        ip,
                        userAgent,
                        correlationId,
                        wrappedResponse.getStatus() < 400,
                        null,
                        null,
                        null,
                        durationMs,
                        Instant.now()
                ));
            } catch (Exception e) {
                log.warn("Failed to record user activity for {} {}: {}", request.getMethod(), path, e.getMessage());
            }
        }
    }

    private boolean shouldSkip(String path) {
        if (path == null || path.equals("/") || path.isEmpty()) return true;
        for (String prefix : SKIPPED_PATHS) {
            if (path.startsWith(prefix)) return true;
        }
        for (String ext : SKIPPED_EXTENSIONS) {
            if (path.endsWith(ext)) return true;
        }
        return false;
    }

    static String determineAction(String httpMethod, int status) {
        if (status >= 500) return "ERROR";
        if (status == 401 || status == 403) return "ACCESS_DENIED";
        if (status == 429) return "RATE_LIMITED";
        if (status == 404) return "NOT_FOUND";
        return switch (httpMethod.toUpperCase()) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    static String guessResourceType(String path) {
        if (path == null) return null;
        String clean = path.startsWith("/") ? path.substring(1) : path;
        String[] parts = clean.split("/");
        if (parts.length >= 4 && "api".equals(parts[0]) && "v1".equals(parts[1])) {
            return parts[2] + "/" + parts[3];
        }
        if (parts.length >= 2) {
            return parts[0];
        }
        return clean;
    }

    private static class StatusCaptureWrapper extends HttpServletResponseWrapper {
        private int capturedStatus = 200;

        StatusCaptureWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int status) {
            this.capturedStatus = status;
            super.setStatus(status);
        }

        @Override
        public void sendError(int status, String error) throws IOException {
            this.capturedStatus = status;
            super.sendError(status, error);
        }

        @Override
        public void sendError(int status) throws IOException {
            this.capturedStatus = status;
            super.sendError(status);
        }

        @Override
        public int getStatus() {
            return capturedStatus;
        }
    }
}
