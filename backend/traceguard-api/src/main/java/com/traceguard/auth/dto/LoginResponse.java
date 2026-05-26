package com.traceguard.auth.dto;

import java.util.UUID;

public record LoginResponse(
                String accessToken,
                String refreshToken,
                String tokenType,
                long expiresIn,
                long refreshExpiresIn,
                UserSummary user) {
        public record UserSummary(
                        UUID id,
                        UUID tenantId,
                        String role) {
        }
}