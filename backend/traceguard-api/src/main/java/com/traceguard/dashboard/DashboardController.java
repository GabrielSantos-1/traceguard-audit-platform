package com.traceguard.dashboard;

import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.dashboard.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    public ResponseEntity<ApiSuccessResponse<DashboardKpiResponse>> getKpi(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var data = dashboardService.getKpi(principal.tenantId());
        var meta = ApiMeta.basic(CorrelationIdUtil.getOrCreate(request));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta));
    }

    @GetMapping("/severity-distribution")
    public ResponseEntity<ApiSuccessResponse<List<SeverityDistributionItem>>> getSeverityDistribution(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var data = dashboardService.getSeverityDistribution(principal.tenantId());
        var meta = ApiMeta.basic(CorrelationIdUtil.getOrCreate(request));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiSuccessResponse<List<TimelineItem>>> getTimeline(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var data = dashboardService.getTimeline(principal.tenantId());
        var meta = ApiMeta.basic(CorrelationIdUtil.getOrCreate(request));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta));
    }

    @GetMapping("/recent-events")
    public ResponseEntity<ApiSuccessResponse<List<RecentEventItem>>> getRecentEvents(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var data = dashboardService.getRecentEvents(principal.tenantId());
        var meta = ApiMeta.basic(CorrelationIdUtil.getOrCreate(request));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta));
    }

    @GetMapping("/critical-alerts")
    public ResponseEntity<ApiSuccessResponse<List<CriticalAlertItem>>> getCriticalAlerts(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var data = dashboardService.getCriticalAlerts(principal.tenantId());
        var meta = ApiMeta.basic(CorrelationIdUtil.getOrCreate(request));
        return ResponseEntity.ok(ApiSuccessResponse.of(data, meta));
    }
}
