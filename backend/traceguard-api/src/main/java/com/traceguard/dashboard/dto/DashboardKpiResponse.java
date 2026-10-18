package com.traceguard.dashboard.dto;

import java.util.List;

public record DashboardKpiResponse(
        long totalEvents24h,
        long activeAlerts,
        long activeUsers,
        long totalTenants,
        Double eventsChangePercent,
        Double alertsChangePercent,
        Double usersChangePercent
) {}
