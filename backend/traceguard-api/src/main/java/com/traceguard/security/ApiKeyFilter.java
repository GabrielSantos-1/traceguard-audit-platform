package com.traceguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health");
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
                response.setStatus(401);
                return;
            }

            var auth = apiKeyService.resolveAuth(apiKey);
            if (auth == null) {
                response.setStatus(401);
                return;
            }

            TenantContext.setTenantId(auth.tenantId());
            TenantContext.setScopes(auth.scopes());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
