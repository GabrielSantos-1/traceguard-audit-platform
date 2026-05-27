package com.traceguard.monitoring.detection;

import com.traceguard.monitoring.detection.dto.AlertQueryRequest;
import com.traceguard.monitoring.detection.dto.AlertResponse;
import com.traceguard.monitoring.detection.dto.BlockedIpResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DetectionService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "severity", "ruleName", "createdAt", "acknowledged"
    );

    private final DetectionAlertRepository alertRepository;
    private final BlockedIpRepository blockedIpRepository;

    public DetectionService(DetectionAlertRepository alertRepository, BlockedIpRepository blockedIpRepository) {
        this.alertRepository = alertRepository;
        this.blockedIpRepository = blockedIpRepository;
    }

    public Page<AlertResponse> listAlerts(AlertQueryRequest request, UUID actorTenantId, boolean isSuperAdmin) {
        Specification<DetectionAlert> spec = Specification.where(null);

        if (!isSuperAdmin) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), actorTenantId));
        }
        if (request.getTenantId() != null && isSuperAdmin) {
            UUID tenantId = UUID.fromString(request.getTenantId());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));
        }
        if (request.getUserId() != null) {
            UUID userId = UUID.fromString(request.getUserId());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ruleName"), request.getRuleName()));
        }
        if (request.getSeverity() != null && !request.getSeverity().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), request.getSeverity().toUpperCase()));
        }
        if (request.getAcknowledged() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("acknowledged"), request.getAcknowledged()));
        }
        if (request.getFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFrom()));
        }
        if (request.getTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), request.getTo()));
        }

        Sort sort = resolveSort(request.getSort());
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        return alertRepository.findAll(spec, pageRequest).map(this::toAlertResponse);
    }

    @Transactional
    public void acknowledgeAlert(UUID alertId, UUID actorUserId) {
        DetectionAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Alert not found: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(actorUserId);
        alert.setAcknowledgedAt(Instant.now());
        alertRepository.save(alert);
    }

    public List<BlockedIpResponse> listBlockedIps(UUID tenantId) {
        return blockedIpRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::toBlockedIpResponse)
                .toList();
    }

    public long countActiveAlerts(UUID tenantId) {
        Specification<DetectionAlert> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.isFalse(root.get("acknowledged"))
        );
        return alertRepository.count(spec);
    }

    private AlertResponse toAlertResponse(DetectionAlert alert) {
        return new AlertResponse(
                alert.getId(), alert.getRuleName(), alert.getSeverity(),
                alert.getTitle(), alert.getDescription(),
                alert.getTenantId(), alert.getUserId(), alert.getSourceIp(),
                alert.getEvidence(), alert.isAcknowledged(),
                alert.getAcknowledgedBy(), alert.getAcknowledgedAt(),
                alert.getCreatedAt()
        );
    }

    private BlockedIpResponse toBlockedIpResponse(BlockedIp blocked) {
        return new BlockedIpResponse(
                blocked.getId(), blocked.getIp(), blocked.getTenantId(),
                blocked.getRuleName(), blocked.getReason(),
                blocked.getBlockedAt(), blocked.getExpiresAt(), blocked.isActive()
        );
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String clean = sort.trim();
        boolean desc = clean.startsWith("-");
        String field = desc ? clean.substring(1) : clean;
        if (!ALLOWED_SORT_FIELDS.contains(field)) field = "createdAt";
        return desc ? Sort.by(Sort.Direction.DESC, field) : Sort.by(Sort.Direction.ASC, field);
    }
}
