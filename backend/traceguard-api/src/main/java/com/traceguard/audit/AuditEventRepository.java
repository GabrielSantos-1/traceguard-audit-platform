package com.traceguard.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByTenantIdAndOccurredAtBetween(
            UUID tenantId,
            Instant start,
            Instant end,
            Pageable pageable
    );

    Page<AuditEvent> findByTenantIdAndSeverityAndOccurredAtBetween(
            UUID tenantId,
            String severity,
            Instant start,
            Instant end,
            Pageable pageable
    );

    Page<AuditEvent> findByTenantIdAndUserIdAndOccurredAtBetween(
            UUID tenantId,
            UUID userId,
            Instant start,
            Instant end,
            Pageable pageable
    );

    Page<AuditEvent> findByTenantIdAndActionAndOccurredAtBetween(
            UUID tenantId,
            String action,
            Instant start,
            Instant end,
            Pageable pageable
    );
}
