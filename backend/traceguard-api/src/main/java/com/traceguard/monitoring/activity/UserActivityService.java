package com.traceguard.monitoring.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.monitoring.activity.dto.ActivityQueryRequest;
import com.traceguard.monitoring.activity.dto.ActivityResponse;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "occurredAt", "action", "resourceType", "userId", "httpMethod", "httpStatus", "durationMs");

    private final UserActivityRepository repository;
    private final InputSanitizer sanitizer;

    public UserActivityService(UserActivityRepository repository, InputSanitizer sanitizer) {
        this.repository = repository;
        this.sanitizer = sanitizer;
    }

    @Async
    public void recordAsync(ActivityRecord record) {
        // tenant_id is NOT NULL in user_activity_log: an event with no tenant (an anonymous request
        // that never authenticated, for example) cannot be attributed to anyone. Skipping it here is
        // correct; letting it through failed the insert on a background thread and filled the log
        // with constraint violations for requests that had already been answered successfully.
        if (record.tenantId() == null) {
            log.debug("event=activity_skipped reason=no_tenant path={}", record.httpPath());
            return;
        }
        try {
            UserActivityEvent event = new UserActivityEvent();
            event.setTenantId(record.tenantId());
            event.setUserId(record.userId());
            event.setUserRole(sanitizeOrNull(record.userRole(), 50, "userRole"));
            event.setAction(sanitizeOrNull(record.action(), 100, "action"));
            event.setResourceType(sanitizeOrNull(record.resourceType(), 100, "resourceType"));
            event.setResourceId(sanitizeOrNull(record.resourceId(), 200, "resourceId"));
            event.setHttpMethod(sanitizeOrNull(record.httpMethod(), 10, "httpMethod"));
            event.setHttpPath(sanitizeOrNull(record.httpPath(), 500, "httpPath"));
            event.setHttpStatus(record.httpStatus());
            event.setIp(sanitizeOrNull(record.ip(), 64, "ip"));
            event.setUserAgent(sanitizeOrNull(record.userAgent(), 512, "userAgent"));
            event.setCorrelationId(sanitizeOrNull(record.correlationId(), 128, "correlationId"));
            event.setSuccess(record.success());
            event.setBeforeSnapshot(record.beforeSnapshot());
            event.setAfterSnapshot(record.afterSnapshot());
            event.setDiffSummary(sanitizeOrNull(record.diffSummary(), 5000, "diffSummary"));
            event.setDurationMs(record.durationMs());
            event.setOccurredAt(record.occurredAt());

            repository.save(event);

            if (log.isDebugEnabled()) {
                log.debug("event=user_activity action={} method={} path={} status={} user={} tenant={} correlationId={}",
                        record.action(), record.httpMethod(), record.httpPath(), record.httpStatus(),
                        record.userId(), record.tenantId(), record.correlationId());
            }
        } catch (Exception e) {
            log.warn("Failed to save user activity: {}", e.getMessage());
        }
    }

    public Page<ActivityResponse> list(ActivityQueryRequest query, JwtPrincipal actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        UUID tenantFilter;
        if (actor.role() == AdminRole.SUPER_ADMIN) {
            tenantFilter = parseUuidOrNull(query.getUserId(), "userId") != null
                    ? null
                    : null;
        } else {
            tenantFilter = actor.tenantId();
        }

        Instant from = query.getFrom() != null ? query.getFrom() : Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = query.getTo() != null ? query.getTo() : Instant.now();
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be greater than or equal to from");
        }

        int page = query.getPage() != null ? Math.max(query.getPage(), 0) : 0;
        int size = query.getSize() != null ? Math.min(Math.max(query.getSize(), 1), 100) : 20;

        String rawSort = query.getSort() != null ? query.getSort() : "-occurredAt";
        Sort.Direction direction = rawSort.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = rawSort.startsWith("-") ? rawSort.substring(1) : rawSort;
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sort must be one of: occurredAt, action, resourceType, userId, httpMethod, httpStatus, durationMs");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<UserActivityEvent> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantFilter != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantFilter));
            }

            UUID userIdFilter = parseUuidOrNull(query.getUserId(), "userId");
            if (userIdFilter != null) {
                predicates.add(cb.equal(root.get("userId"), userIdFilter));
            }

            String action = sanitizeOrNull(query.getAction(), 100, "action");
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            String resourceType = sanitizeOrNull(query.getResourceType(), 100, "resourceType");
            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }

            if (query.getSuccess() != null) {
                predicates.add(cb.equal(root.get("success"), query.getSuccess()));
            }

            predicates.add(cb.between(root.get("occurredAt"), from, to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    private ActivityResponse toResponse(UserActivityEvent event) {
        return new ActivityResponse(
                event.getId(),
                event.getTenantId(),
                event.getUserId(),
                event.getUserRole(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getHttpMethod(),
                event.getHttpPath(),
                event.getHttpStatus(),
                event.getIp(),
                event.getCorrelationId(),
                event.isSuccess(),
                event.getBeforeSnapshot(),
                event.getAfterSnapshot(),
                event.getDiffSummary(),
                event.getDurationMs(),
                event.getOccurredAt()
        );
    }

    private String sanitizeOrNull(String value, int maxLength, String fieldName) {
        String sanitized = sanitizer.sanitizeText(value, maxLength, fieldName);
        return sanitized == null || sanitized.isBlank() ? null : sanitized;
    }

    private UUID parseUuidOrNull(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) return null;
        String sanitized = sanitizeOrNull(raw, 64, fieldName);
        if (sanitized == null) return null;
        try {
            return UUID.fromString(sanitized);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID");
        }
    }

    public record ActivityRecord(
            UUID tenantId,
            UUID userId,
            String userRole,
            String action,
            String resourceType,
            String resourceId,
            String httpMethod,
            String httpPath,
            int httpStatus,
            String ip,
            String userAgent,
            String correlationId,
            boolean success,
            JsonNode beforeSnapshot,
            JsonNode afterSnapshot,
            String diffSummary,
            Long durationMs,
            Instant occurredAt
    ) {}
}
