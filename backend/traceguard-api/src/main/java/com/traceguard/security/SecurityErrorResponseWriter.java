package com.traceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.common.api.ApiErrorBody;
import com.traceguard.common.api.ApiErrorResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

import static com.traceguard.auth.AuthObservability.*;

public final class SecurityErrorResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorResponseWriter.class);

    private SecurityErrorResponseWriter() {
    }

    public static void write(
            ObjectMapper mapper,
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        String correlationId = CorrelationIdUtil.getOrCreate(request);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationIdUtil.HEADER_NAME, correlationId);

        ApiErrorBody body = new ApiErrorBody(
                code,
                message,
                Instant.now(),
                request.getRequestURI(),
                correlationId,
                null
        );
        if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN) {
            String eventName = status == HttpServletResponse.SC_UNAUTHORIZED ? EVENT_AUTH_UNAUTHORIZED : EVENT_AUTH_FORBIDDEN;
            String reasonCode = status == HttpServletResponse.SC_UNAUTHORIZED ? REASON_UNAUTHORIZED : REASON_FORBIDDEN;
            log.warn(
                    "event={} outcome={} reasonCode={} code={} status={} path={} method={} correlationId={}",
                    eventName,
                    OUTCOME_FAILURE,
                    reasonCode,
                    code,
                    status,
                    request.getRequestURI(),
                    request.getMethod(),
                    correlationId
            );
        }
        mapper.writeValue(response.getWriter(), ApiErrorResponse.of(body));
    }
}
