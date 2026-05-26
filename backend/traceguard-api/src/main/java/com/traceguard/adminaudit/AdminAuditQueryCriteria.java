package com.traceguard.adminaudit;

import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

record AdminAuditQueryCriteria(
        UUID tenantId,
        UUID adminId,
        String action,
        String resourceType,
        String resourceId,
        Instant from,
        Instant to,
        int page,
        int size,
        String sortField,
        Sort.Direction sortDirection
) {
}
