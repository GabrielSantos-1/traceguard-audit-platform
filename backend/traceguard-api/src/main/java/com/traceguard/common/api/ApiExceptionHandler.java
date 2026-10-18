package com.traceguard.common.api;

import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> details.put(err.getField(), err.getDefaultMessage()));
        log.warn("event=validation_failed path={} details={}", request.getRequestURI(), details.keySet());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Validation failed", details, request);
    }

    @ExceptionHandler({
            BindException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInputErrors(Exception ex, HttpServletRequest request) {
        log.warn("event=validation_failed path={} type={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request input", null, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = mapErrorCode(status);
        String message = safeMessage(status, ex.getReason());
        if (status.is4xxClientError()) {
            log.warn("event=handled_error code={} path={} status={}", code, request.getRequestURI(), status.value());
        } else {
            log.error("event=handled_error code={} path={} status={}", code, request.getRequestURI(), status.value());
        }
        return buildError(status, code, message, null, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        // A missing static file (favicon, stray asset) or an unmapped path is a 404, not a server
        // failure. Falling through to the generic handler turned every wrong URL into a 500.
        log.warn("event=not_found path={} type={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("event=internal_error path={} type={}", request.getRequestURI(), ex.getClass().getSimpleName(), ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected internal error",
                null,
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> details,
            HttpServletRequest request
    ) {
        ApiErrorBody body = new ApiErrorBody(
                code,
                message,
                Instant.now(),
                request.getRequestURI(),
                CorrelationIdUtil.getOrCreate(request),
                details
        );
        return ResponseEntity.status(status).body(ApiErrorResponse.of(body));
    }

    private String mapErrorCode(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "BAD_REQUEST";
            case TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
            default -> "INTERNAL_ERROR";
        };
    }

    private String safeMessage(HttpStatus status, String reason) {
        if (reason == null || reason.isBlank()) {
            return switch (status) {
                case UNAUTHORIZED -> "Authentication is required";
                case FORBIDDEN -> "Operation is not allowed";
                case NOT_FOUND -> "Resource not found";
                case CONFLICT -> "Request conflicts with current state";
                case BAD_REQUEST, UNPROCESSABLE_ENTITY -> "Invalid request";
                case TOO_MANY_REQUESTS -> "Rate limit exceeded";
                default -> "Unexpected internal error";
            };
        }
        return reason;
    }
}
