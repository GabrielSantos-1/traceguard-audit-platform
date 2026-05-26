package com.traceguard.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.security.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiter;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final int loginPerMinute;
    private final int refreshPerMinute;
    private final int auditIngestPerMinute;
    private final int auditQueryPerMinute;
    private final int adminReadPerMinute;
    private final int adminWritePerMinute;

    public RateLimitFilter(
            RateLimiterService rateLimiter,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            int loginPerMinute,
            int refreshPerMinute,
            int auditIngestPerMinute,
            int auditQueryPerMinute,
            int adminReadPerMinute,
            int adminWritePerMinute) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.loginPerMinute = loginPerMinute;
        this.refreshPerMinute = refreshPerMinute;
        this.auditIngestPerMinute = auditIngestPerMinute;
        this.auditQueryPerMinute = auditQueryPerMinute;
        this.adminReadPerMinute = adminReadPerMinute;
        this.adminWritePerMinute = adminWritePerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        RateLimitPolicy policy = resolvePolicy(request);

        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildRateLimitKey(request, policy.bucket());
        RateLimitDecision decision = rateLimiter.check(key, policy);

        applyHeaders(response, decision);

        String outcome = decision.allowed() ? "allowed" : "blocked";
        meterRegistry.counter(
                "traceguard.ratelimit." + outcome,
                "bucket", policy.bucket(),
                "outcome", outcome,
                "source", decision.source()).increment();

        if (!decision.allowed()) {
            String correlationId = CorrelationIdUtil.getOrCreate(request);

            log.warn(
                    "event=RATE_LIMIT_BLOCKED bucket={} keyHash={} path={} method={} source={} outcome={} correlationId={}",
                    policy.bucket(),
                    shortHash(key),
                    request.getRequestURI(),
                    request.getMethod(),
                    decision.source(),
                    outcome,
                    correlationId);

            response.setStatus(429);
            response.setContentType("application/json");

            objectMapper.writeValue(
                    response.getWriter(),
                    Map.of(
                            "success", false,
                            "error", Map.of(
                                    "code", "RATE_LIMIT_EXCEEDED",
                                    "message", "Too many requests"),
                            "meta", Map.of(
                                    "correlationId", correlationId)));
            return;
        }

        log.debug(
                "event=RATE_LIMIT_ALLOWED bucket={} keyHash={} method={} path={} source={} outcome={}",
                policy.bucket(),
                shortHash(key),
                request.getMethod(),
                request.getRequestURI(),
                decision.source(),
                outcome);

        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && "/api/v1/auth/login".equals(path)) {
            return new RateLimitPolicy(loginPerMinute, "login");
        }

        if ("POST".equals(method) && "/api/v1/auth/refresh".equals(path)) {
            return new RateLimitPolicy(refreshPerMinute, "refresh");
        }

        if ("POST".equals(method) && "/api/v1/audit/events".equals(path)) {
            return new RateLimitPolicy(auditIngestPerMinute, "audit_ingest");
        }

        if ("GET".equals(method) && "/api/v1/audit/events".equals(path)) {
            return new RateLimitPolicy(auditQueryPerMinute, "audit_query");
        }

        if (path.startsWith("/api/v1/admin/")) {
            if ("GET".equals(method)) {
                return new RateLimitPolicy(adminReadPerMinute, "admin_read");
            }
            return new RateLimitPolicy(adminWritePerMinute, "admin_write");
        }

        return null;
    }

    private String buildRateLimitKey(HttpServletRequest request, String bucket) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;

        if (principal instanceof JwtPrincipal jwtPrincipal) {
            if ("admin_read".equals(bucket) || "admin_write".equals(bucket)) {
                return jwtPrincipal.tenantId() + ":" + jwtPrincipal.userId();
            }
            if ("audit_query".equals(bucket) || "audit_ingest".equals(bucket)) {
                return jwtPrincipal.tenantId().toString();
            }
        }

        if ("audit_query".equals(bucket) || "audit_ingest".equals(bucket)) {
            java.util.UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                return tenantId.toString();
            }
            return "missing_tenant";
        }

        String remoteIp = request.getRemoteAddr();
        if (remoteIp == null || remoteIp.isBlank()) {
            remoteIp = "unknown";
        }
        return remoteIp;
    }

    private void applyHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed).substring(0, 12);
        } catch (Exception ex) {
            return "unavailable";
        }
    }
}
