package com.traceguard.dashboard.dto;

public record SeverityDistributionItem(
        String severity,
        long count,
        double percentage
) {}
