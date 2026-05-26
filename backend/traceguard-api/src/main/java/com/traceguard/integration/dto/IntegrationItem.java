package com.traceguard.integration.dto;

public record IntegrationItem(
        String provider,
        String name,
        String configJson,
        boolean enabled
) {}
