package com.traceguard.monitoring.detection;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.monitoring.detection.dto.AlertQueryRequest;
import com.traceguard.monitoring.detection.dto.AlertResponse;
import com.traceguard.monitoring.detection.dto.BlockedIpResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/detection")
public class DetectionController {

    private final DetectionService detectionService;

    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiSuccessResponse<List<AlertResponse>>> listAlerts(
            @Valid AlertQueryRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        boolean isSuperAdmin = principal != null && principal.role() == AdminRole.SUPER_ADMIN;
        UUID tenantId = principal != null ? principal.tenantId() : null;

        Page<AlertResponse> page = detectionService.listAlerts(request, tenantId, isSuperAdmin);

        ApiMeta meta = ApiMeta.paged(
                null,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );

        return ResponseEntity.ok(ApiSuccessResponse.of(page.getContent(), meta));
    }

    @PatchMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID actorUserId = principal != null ? principal.userId() : null;
        detectionService.acknowledgeAlert(id, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked-ips")
    public ResponseEntity<ApiSuccessResponse<List<BlockedIpResponse>>> listBlockedIps(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID tenantId = principal != null ? principal.tenantId() : null;
        List<BlockedIpResponse> ips = detectionService.listBlockedIps(tenantId);

        ApiMeta meta = ApiMeta.paged(null, 0, ips.size(), ips.size(), 1, false, false);
        return ResponseEntity.ok(ApiSuccessResponse.of(ips, meta));
    }

    @GetMapping("/alerts/count")
    public ResponseEntity<Long> countActiveAlerts(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID tenantId = principal != null ? principal.tenantId() : null;
        return ResponseEntity.ok(detectionService.countActiveAlerts(tenantId));
    }
}
