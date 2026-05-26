package com.traceguard.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
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

    long countByTenantIdAndOccurredAtBetween(UUID tenantId, Instant start, Instant end);

    @Query("SELECT e.severity, COUNT(e) FROM AuditEvent e " +
           "WHERE e.tenantId = :tenantId AND e.occurredAt BETWEEN :start AND :end " +
           "GROUP BY e.severity")
    List<Object[]> countBySeverityGrouped(UUID tenantId, Instant start, Instant end);

    long countByTenantIdAndSeverityInAndOccurredAtBetween(
            UUID tenantId, List<String> severities, Instant start, Instant end);

    Page<AuditEvent> findByTenantIdAndSeverityInAndOccurredAtBetween(
            UUID tenantId, List<String> severities, Instant start, Instant end, Pageable pageable);

    List<AuditEvent> findByTenantIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            UUID tenantId, Instant start, Instant end);
}
