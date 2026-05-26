package com.traceguard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.security.SecurityErrorResponseWriter;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;
    private final MeterRegistry meterRegistry;

    public JwtAuthFilter(
            JwtTokenService jwtTokenService,
            ObjectMapper objectMapper,
            TenantRepository tenantRepository,
            MeterRegistry meterRegistry) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
        this.tenantRepository = tenantRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = resolvePath(request);
        return !path.startsWith("/api/v1/admin");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        String correlationId = CorrelationIdUtil.getOrCreate(request);
        try {
            JwtPrincipal principal = jwtTokenService.parse(token, correlationId);
            if (principal.role() != AdminRole.SUPER_ADMIN && !isTenantActive(principal.tenantId())) {
                meterRegistry.counter("traceguard.tenant.blocked", "source", "jwt").increment();
                log.warn(
                        "event=TENANT_SUSPENDED outcome=failure reasonCode=tenant_suspended source=jwt tenantId={} userId={} correlationId={}",
                        principal.tenantId(),
                        principal.userId(),
                        correlationId);
                SecurityContextHolder.clearContext();
                SecurityErrorResponseWriter.write(
                        objectMapper,
                        request,
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN",
                        "Operation is not allowed"
                );
                return;
            }
            var authorities = Set.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponseWriter.write(
                    objectMapper,
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Invalid or expired token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        String requestUri = request.getRequestURI();
        return requestUri == null ? "" : requestUri;
    }

    private boolean isTenantActive(java.util.UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }
}
