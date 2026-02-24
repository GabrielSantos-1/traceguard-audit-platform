package com.traceguard.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.audit.dto.AuditEventResponse;
import com.traceguard.audit.dto.CreateAuditEventRequest;
import com.traceguard.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository repo;
    private final ObjectMapper mapper;

    public AuditEventService(AuditEventRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public UUID create(CreateAuditEventRequest req, String ip, String userAgent) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        AuditEvent e = new AuditEvent();
        e.setTenantId(tenantId);

        if (req.userId() != null && !req.userId().isBlank()) {
            e.setUserId(UUID.fromString(req.userId()));
        }

        e.setAction(req.action());
        e.setEntity(req.entity());
        e.setEntityId(req.entityId());
        e.setSeverity(req.severity());
        e.setIp(ip);
        e.setUserAgent(userAgent);

        e.setMetadata(parseJsonOrNull(req.metadataJson()));
        e.setBeforeData(parseJsonOrNull(req.beforeJson()));
        e.setAfterData(parseJsonOrNull(req.afterJson()));

        return repo.save(e).getId();
    }

    public Page<AuditEventResponse> list(
            String severity,
            String userId,
            String action,
            Instant start,
            Instant end,
            int page,
            int size
    ) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }

        Instant s = (start != null) ? start : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant e = (end != null) ? end : Instant.now();

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        Page<AuditEvent> result;

        if (severity != null && !severity.isBlank()) {
            result = repo.findByTenantIdAndSeverityAndOccurredAtBetween(tenantId, severity, s, e, pageable);
        } else if (userId != null && !userId.isBlank()) {
            result = repo.findByTenantIdAndUserIdAndOccurredAtBetween(tenantId, UUID.fromString(userId), s, e, pageable);
        } else if (action != null && !action.isBlank()) {
            result = repo.findByTenantIdAndActionAndOccurredAtBetween(tenantId, action, s, e, pageable);
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
        if (raw == null || raw.isBlank()) return null;
        try {
            return mapper.readTree(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }
    }
}
