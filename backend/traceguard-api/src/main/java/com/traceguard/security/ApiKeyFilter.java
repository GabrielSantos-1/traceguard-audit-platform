package com.traceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(ApiKeyService apiKeyService, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolvePath(request);
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/admin");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TenantContext.clear();
        try {
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey == null || apiKey.isBlank()) {
                SecurityErrorResponseWriter.write(
                        objectMapper,
                        request,
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication is required"
                );
                return;
            }

            String correlationId = CorrelationIdUtil.getOrCreate(request);
            ApiKeyValidationResult validation = apiKeyService.resolveAuthDetailed(apiKey, correlationId);
            if (!validation.isAuthenticated()) {
                int status = HttpServletResponse.SC_UNAUTHORIZED;
                String code = "UNAUTHORIZED";
                String message = "Invalid API key";

                if (validation.failureReason() == ApiKeyValidationFailureReason.TENANT_SUSPENDED) {
                    status = HttpServletResponse.SC_FORBIDDEN;
                    code = "FORBIDDEN";
                    message = "Operation is not allowed";
                }

                SecurityErrorResponseWriter.write(
                        objectMapper,
                        request,
                        response,
                        status,
                        code,
                        message
                );
                return;
            }
            var auth = validation.auth();

            TenantContext.setTenantId(auth.tenantId());
            TenantContext.setScopes(auth.scopes());

            var authorities = auth.scopes().stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.name()))
                    .collect(Collectors.toSet());
            var authentication = new UsernamePasswordAuthenticationToken("api-key", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    private String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        String requestUri = request.getRequestURI();
        return requestUri == null ? "" : requestUri;
    }
}
