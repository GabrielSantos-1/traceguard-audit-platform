package com.traceguard.common.web;

import com.traceguard.security.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RequestCorrelationLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationLoggingFilter.class);
    private final MeterRegistry meterRegistry;

    public RequestCorrelationLoggingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String correlationId = CorrelationIdUtil.getOrCreate(request);
        response.setHeader(CorrelationIdUtil.HEADER_NAME, correlationId);

        log.info(
                "event=request_started method={} path={} correlationId={} remoteIp={}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId,
                request.getRemoteAddr()
        );
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.info(
                    "event=request_finished method={} path={} status={} durationMs={} correlationId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    durationMs,
                    correlationId
            );
            meterRegistry.counter("traceguard.http.requests.total", "status", Integer.toString(status)).increment();
            TenantContext.clear();
        }
    }
}
