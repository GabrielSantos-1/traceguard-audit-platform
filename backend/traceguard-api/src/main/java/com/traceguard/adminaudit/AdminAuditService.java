package com.traceguard.adminaudit;

import com.traceguard.adminaudit.dto.AdminAuditEventResponse;
import com.traceguard.adminaudit.dto.AdminAuditQueryRequest;
import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("occurredAt", "action", "resourceType", "adminId", "tenantId");

    private final AdminAuditEventRepository repository;
    private final AuditSanitizer auditSanitizer;
    private final InputSanitizer sanitizer;

    public AdminAuditService(
            AdminAuditEventRepository repository,
            AuditSanitizer auditSanitizer,
            InputSanitizer sanitizer
    ) {
        this.repository = repository;
        this.auditSanitizer = auditSanitizer;
        this.sanitizer = sanitizer;
    }

    public void record(
            UUID adminId,
            UUID tenantId,
            String action,
            String resourceType,
            String resourceId,
            String correlationId,
            Map<String, Object> details
    ) {
        AdminAuditEvent event = new AdminAuditEvent();
        event.setAdminId(adminId);
        event.setTenantId(tenantId);
        event.setAction(sanitizer.sanitizeText(action, 120, "action"));
        event.setResourceType(sanitizer.sanitizeText(resourceType, 80, "resourceType"));
        event.setResourceId(sanitizer.sanitizeText(resourceId, 120, "resourceId"));
        event.setCorrelationId(sanitizer.sanitizeText(correlationId, 128, "correlationId"));
        event.setDetailsJson(auditSanitizer.sanitizeForPersistence(details));
        repository.save(event);

        log.info(
                "event=admin_audit action={} resourceType={} resourceId={} adminId={} tenantId={} correlationId={}",
                action, resourceType, resourceId, adminId, tenantId, correlationId
        );
    }

    public Page<AdminAuditEventResponse> list(AdminAuditQueryRequest query, JwtPrincipal actor) {
        AdminAuditQueryCriteria criteria = resolveCriteria(query, actor);
        PageRequest pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(criteria.sortDirection(), criteria.sortField()));

        Specification<AdminAuditEvent> specification = Specification.where(null);

        if (criteria.tenantId() != null) {
            specification = specification.and((root, cq, cb) -> cb.equal(root.get("tenantId"), criteria.tenantId()));
        }
        if (criteria.adminId() != null) {
            specification = specification.and((root, cq, cb) -> cb.equal(root.get("adminId"), criteria.adminId()));
        }
        if (criteria.action() != null) {
            specification = specification.and((root, cq, cb) -> cb.equal(root.get("action"), criteria.action()));
        }
        if (criteria.resourceType() != null) {
            specification = specification.and((root, cq, cb) -> cb.equal(root.get("resourceType"), criteria.resourceType()));
        }
        if (criteria.resourceId() != null) {
            specification = specification.and((root, cq, cb) -> cb.equal(root.get("resourceId"), criteria.resourceId()));
        }

        specification = specification.and((root, cq, cb) -> cb.between(root.get("occurredAt"), criteria.from(), criteria.to()));

        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    AdminAuditQueryCriteria resolveCriteria(AdminAuditQueryRequest query, JwtPrincipal actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (actor.role() != AdminRole.SUPER_ADMIN && actor.role() != AdminRole.TENANT_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role cannot list admin audit events");
        }

        UUID tenantFilter;
        if (actor.role() == AdminRole.SUPER_ADMIN) {
            tenantFilter = parseUuidOrNull(query.getTenantId(), "tenantId");
        } else {
            tenantFilter = actor.tenantId();
        }

        UUID adminFilter = parseUuidOrNull(query.getAdminId(), "adminId");
        String action = sanitizeOrNull(query.getAction(), 120, "action");
        String resourceType = sanitizeOrNull(query.getResourceType(), 80, "resourceType");
        String resourceId = sanitizeOrNull(query.getResourceId(), 120, "resourceId");

        Instant from = query.getFrom() != null ? query.getFrom() : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant to = query.getTo() != null ? query.getTo() : Instant.now();
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must be greater than or equal to from");
        }

        int page = query.getPage() != null ? Math.max(query.getPage(), 0) : 0;
        int size = query.getSize() != null ? Math.min(Math.max(query.getSize(), 1), 100) : 20;

        String rawSort = sanitizeOrNull(query.getSort(), 40, "sort");
        if (rawSort == null) {
            rawSort = "-occurredAt";
        }
        Sort.Direction direction = rawSort.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = rawSort.startsWith("-") ? rawSort.substring(1) : rawSort;
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "sort must be one of: occurredAt, action, resourceType, adminId, tenantId");
        }

        return new AdminAuditQueryCriteria(
                tenantFilter,
                adminFilter,
                action,
                resourceType,
                resourceId,
                from,
                to,
                page,
                size,
                sortField,
                direction);
    }

    private AdminAuditEventResponse toResponse(AdminAuditEvent event) {
        return new AdminAuditEventResponse(
                event.getId(),
                event.getAdminId(),
                event.getTenantId(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCorrelationId(),
                event.getOccurredAt(),
                auditSanitizer.sanitizeForResponse(event.getDetailsJson()));
    }

    private String sanitizeOrNull(String value, int maxLength, String fieldName) {
        String sanitized = sanitizer.sanitizeText(value, maxLength, fieldName);
        return sanitized == null || sanitized.isBlank() ? null : sanitized;
    }

    private UUID parseUuidOrNull(String raw, String fieldName) {
        String sanitized = sanitizeOrNull(raw, 64, fieldName);
        if (sanitized == null) {
            return null;
        }
        try {
            return UUID.fromString(sanitized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a valid UUID");
        }
    }
}
