package com.traceguard.integration.dto;

public record SaveIntegrationRequest(
        String name,
        String configJson,
        boolean enabled
) {}
