package com.traceguard.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.audit.dto.AuditEventResponse;
import com.traceguard.audit.dto.CreateAuditEventRequest;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.security.TenantContext;
import com.traceguard.tenant.TenantRepository;
import com.traceguard.tenant.TenantStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("occurredAt", "severity", "action", "userId");

    private final AuditEventRepository repo;
    private final ObjectMapper mapper;
    private final InputSanitizer sanitizer;
    private final MeterRegistry meterRegistry;
    private final TenantRepository tenantRepository;

    public AuditEventService(
            AuditEventRepository repo,
            ObjectMapper mapper,
            InputSanitizer sanitizer,
            MeterRegistry meterRegistry,
            TenantRepository tenantRepository
    ) {
        this.repo = repo;
        this.mapper = mapper;
        this.sanitizer = sanitizer;
        this.meterRegistry = meterRegistry;
        this.tenantRepository = tenantRepository;
    }

    public UUID create(CreateAuditEventRequest req, String ip, String userAgent) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }
        if (!isTenantActive(tenantId)) {
            meterRegistry.counter("traceguard.tenant.blocked", "source", "audit").increment();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operation is not allowed");
        }
        if (!TenantContext.hasScope(com.traceguard.security.ApiKeyScope.AUDIT_WRITE)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Missing scope"
            );
        }

        AuditEvent e = new AuditEvent();
        e.setTenantId(tenantId);

        if (req.userId() != null && !req.userId().isBlank()) {
            e.setUserId(parseUuidOrBadRequest(req.userId(), "userId"));
        }

        e.setAction(sanitizer.sanitizeText(req.action(), 100, "action"));
        e.setEntity(sanitizer.sanitizeText(req.entity(), 100, "entity"));
        e.setEntityId(sanitizer.sanitizeText(req.entityId(), 100, "entityId"));
        e.setSeverity(sanitizer.sanitizeText(req.severity(), 50, "severity"));
        e.setIp(sanitizer.sanitizeText(ip, 100, "ip"));
        e.setUserAgent(sanitizer.sanitizeText(userAgent, 255, "userAgent"));

        e.setMetadata(parseJsonOrNull(req.metadataJson()));
        e.setBeforeData(parseJsonOrNull(req.beforeJson()));
        e.setAfterData(parseJsonOrNull(req.afterJson()));

        UUID id = repo.save(e).getId();
        meterRegistry.counter("traceguard.audit.events.ingested").increment();
        log.info("event=audit_event_persisted eventId={} tenantId={} action={} severity={}", id, tenantId, e.getAction(), e.getSeverity());
        return id;
    }

    public Page<AuditEventResponse> list(
            String severity,
            String userId,
            String action,
            Instant start,
            Instant end,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }
        if (!isTenantActive(tenantId)) {
            meterRegistry.counter("traceguard.tenant.blocked", "source", "audit").increment();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operation is not allowed");
        }
        if (!TenantContext.hasScope(com.traceguard.security.ApiKeyScope.AUDIT_READ)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Missing scope"
            );
        }

        Instant s = (start != null) ? start : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant e = (end != null) ? end : Instant.now();

        if (e.isBefore(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be greater than or equal to start");
        }

        String normalizedSortField = normalizeSortField(sortBy);
        Sort.Direction direction = normalizeSortDirection(sortDirection);

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(direction, normalizedSortField)
        );

        Page<AuditEvent> result;

        if (severity != null && !severity.isBlank()) {
            String sanitizedSeverity = sanitizer.sanitizeText(severity, 50, "severity");
            result = repo.findByTenantIdAndSeverityAndOccurredAtBetween(tenantId, sanitizedSeverity, s, e, pageable);
        } else if (userId != null && !userId.isBlank()) {
            UUID sanitizedUserId = parseUuidOrBadRequest(userId, "userId");
            result = repo.findByTenantIdAndUserIdAndOccurredAtBetween(tenantId, sanitizedUserId, s, e, pageable);
        } else if (action != null && !action.isBlank()) {
            String sanitizedAction = sanitizer.sanitizeText(action, 100, "action");
            result = repo.findByTenantIdAndActionAndOccurredAtBetween(tenantId, sanitizedAction, s, e, pageable);
        } else {
            result = repo.findByTenantIdAndOccurredAtBetween(tenantId, s, e, pageable);
        }

        return result.map(this::toResponse);
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return new AuditEventResponse(
                e.getId().toString(),
                e.getTenantId().toString(),
                e.getUserId() != null ? e.getUserId().toString() : null,
                e.getAction(),
                e.getEntity(),
                e.getEntityId(),
                e.getSeverity(),
                e.getIp(),
                e.getUserAgent(),
                e.getOccurredAt()
        );
    }

    private JsonNode parseJsonOrNull(String raw) {
        String sanitizedPayload = sanitizer.sanitizeJsonPayload(raw, 10000, "jsonPayload");
        if (sanitizedPayload == null) {
            return null;
        }

        try {
            return mapper.readTree(sanitizedPayload);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload");
        }
    }

    private UUID parseUuidOrBadRequest(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID");
        }
    }

    private String normalizeSortField(String value) {
        String candidate = sanitizer.sanitizeText(value, 30, "sortBy");
        if (candidate == null || candidate.isBlank()) {
            return "occurredAt";
        }
        if (!ALLOWED_SORT_FIELDS.contains(candidate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "sortBy must be one of: occurredAt, severity, action, userId"
            );
        }
        return candidate;
    }

    private Sort.Direction normalizeSortDirection(String value) {
        String candidate = sanitizer.sanitizeText(value, 10, "sortDirection");
        if (candidate == null || candidate.isBlank()) {
            return Sort.Direction.DESC;
        }

        return switch (candidate.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "sortDirection must be one of: asc, desc"
            );
        };
    }

    private boolean isTenantActive(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }
}
