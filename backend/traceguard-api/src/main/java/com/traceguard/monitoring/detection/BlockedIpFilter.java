package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.common.util.IpResolver;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class BlockedIpFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BlockedIpFilter.class);
    private static final Set<String> SKIPPED_PATHS = Set.of(
            "/actuator/", "/swagger-ui", "/v3/api-docs", "/h2-console",
            "/index.html", "/favicon.ico"
    );

    private final BlockedIpRepository blockedIpRepository;
    private final ObjectMapper objectMapper;

    public BlockedIpFilter(BlockedIpRepository blockedIpRepository, ObjectMapper objectMapper) {
        this.blockedIpRepository = blockedIpRepository;
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

        String ip = IpResolver.resolve(request);
        if (ip == null || ip.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        var activeBlocks = blockedIpRepository.findByIpAndActiveTrue(ip);

        if (!activeBlocks.isEmpty()) {
            String correlationId = CorrelationIdUtil.getOrCreate(request);
            log.warn("Blocked request from IP {} ({} active blocks) - {}", ip, activeBlocks.size(), path);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String json = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "success", false,
                            "error", java.util.Map.of(
                                    "code", "IP_BLOCKED",
                                    "message", "Your IP address has been blocked due to suspicious activity",
                                    "correlationId", correlationId
                            )
                    )
            );
            response.getWriter().write(json);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        if (path == null || path.isBlank()) return true;
        String clean = path.startsWith("/") ? path : "/" + path;
        for (String prefix : SKIPPED_PATHS) {
            if (clean.startsWith(prefix)) return true;
        }
        return false;
    }
}
