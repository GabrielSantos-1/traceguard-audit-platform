package com.traceguard.auth;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.dto.RefreshTokenResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.traceguard.auth.AuthObservability.*;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenService jwtTokenService;
    private final AdminAuditService adminAuditService;
    private final MeterRegistry meterRegistry;
    private final long refreshTokenDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            JwtTokenService jwtTokenService,
            AdminAuditService adminAuditService,
            MeterRegistry meterRegistry,
            @Value("${traceguard.jwt.refresh-token-days}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenService = jwtTokenService;
        this.adminAuditService = adminAuditService;
        this.meterRegistry = meterRegistry;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public IssuedRefreshToken issueForLogin(UserAccount user, String correlationId, String ipAddress,
            String userAgent) {
        String rawToken = generateRawToken();
        RefreshTokenRecord record = new RefreshTokenRecord();
        record.setUserId(user.getId());
        record.setTenantId(user.getTenantId());
        record.setFamilyId(UUID.randomUUID());
        record.setTokenHash(hashToken(rawToken));
        record.setTokenPrefix(prefixOf(rawToken));
        record.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshTokenDays)));
        record.setCreatedIp(truncate(ipAddress, 64));
        record.setCreatedUserAgent(truncate(userAgent, 512));

        RefreshTokenRecord saved = refreshTokenRepository.save(record);

        adminAuditService.record(
                user.getId(),
                user.getTenantId(),
                "ADMIN_REFRESH_TOKEN_ISSUED",
                "REFRESH_TOKEN",
                saved.getId().toString(),
                correlationId,
                Map.of("familyId", saved.getFamilyId().toString()));

        return new IssuedRefreshToken(rawToken, saved);
    }

    @Transactional
    public RefreshTokenResponse refresh(String rawRefreshToken, String correlationId, String ipAddress,
            String userAgent) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshTokenRecord current = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn(
                            "event={} outcome={} reasonCode={} correlationId={}",
                            EVENT_AUTH_REFRESH_FAILURE,
                            OUTCOME_FAILURE,
                            REASON_INVALID_REFRESH_TOKEN,
                            correlationId);
                    return unauthorized("Invalid refresh token", REASON_INVALID_REFRESH_TOKEN);
                });

        Instant now = Instant.now();

        if (current.isExpired(now)) {
            revokeToken(current, "EXPIRED", now);
            log.warn(
                    "event={} outcome={} reasonCode={} userId={} tenantId={} correlationId={}",
                    EVENT_AUTH_REFRESH_FAILURE,
                    OUTCOME_FAILURE,
                    REASON_EXPIRED_TOKEN,
                    current.getUserId(),
                    current.getTenantId(),
                    correlationId);
            throw unauthorized("Refresh token expired", REASON_EXPIRED_TOKEN);
        }

        if (current.isRevoked() || current.isReplaced()) {
            current.setReuseDetectedAt(now);
            refreshTokenRepository.save(current);
            revokeFamily(current.getFamilyId(), now, "REUSE_DETECTED");

            log.warn(
                    "event={} outcome={} reasonCode={} userId={} tenantId={} correlationId={}",
                    EVENT_AUTH_REFRESH_REUSE_DETECTED,
                    OUTCOME_FAILURE,
                    REASON_REUSE_DETECTED,
                    current.getUserId(),
                    current.getTenantId(),
                    correlationId);
            incrementAuthCounter("traceguard.auth.refresh.failed", FLOW_REFRESH, OUTCOME_FAILURE,
                    REASON_REUSE_DETECTED);
            incrementAuthCounter("traceguard.auth.refresh.reuse_detected", FLOW_REFRESH, OUTCOME_FAILURE,
                    REASON_REUSE_DETECTED);

            adminAuditService.record(
                    current.getUserId(),
                    current.getTenantId(),
                    "ADMIN_REFRESH_TOKEN_REUSE_DETECTED",
                    "REFRESH_TOKEN",
                    current.getId().toString(),
                    correlationId,
                    Map.of(
                            "familyId", current.getFamilyId().toString(),
                            "ipAddress", safe(ipAddress),
                            "userAgent", safe(truncate(userAgent, 512))));

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected");
        }

        UserAccount user = userAccountRepository.findById(current.getUserId())
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn(
                            "event={} outcome={} reasonCode={} userId={} tenantId={} correlationId={}",
                            EVENT_AUTH_REFRESH_FAILURE,
                            OUTCOME_FAILURE,
                            REASON_USER_INACTIVE,
                            current.getUserId(),
                            current.getTenantId(),
                            correlationId);
                    return unauthorized("User not available", REASON_USER_INACTIVE);
                });

        String newRawToken = generateRawToken();

        RefreshTokenRecord replacement = new RefreshTokenRecord();
        replacement.setUserId(user.getId());
        replacement.setTenantId(user.getTenantId());
        replacement.setFamilyId(current.getFamilyId());
        replacement.setTokenHash(hashToken(newRawToken));
        replacement.setTokenPrefix(prefixOf(newRawToken));
        replacement.setExpiresAt(now.plus(Duration.ofDays(refreshTokenDays)));
        replacement.setCreatedIp(truncate(ipAddress, 64));
        replacement.setCreatedUserAgent(truncate(userAgent, 512));

        RefreshTokenRecord savedReplacement = refreshTokenRepository.save(replacement);

        current.setLastUsedAt(now);
        current.setRevokedAt(now);
        current.setRevocationReason("ROTATED");
        current.setReplacedByTokenId(savedReplacement.getId());
        refreshTokenRepository.save(current);

        String accessToken = jwtTokenService.issueAccessToken(user);

        incrementAuthCounter("traceguard.auth.refresh.success", FLOW_REFRESH, OUTCOME_SUCCESS, REASON_NONE);
        log.info(
                "event={} outcome={} reasonCode={} userId={} tenantId={} role={} correlationId={}",
                EVENT_AUTH_REFRESH_SUCCESS,
                OUTCOME_SUCCESS,
                REASON_NONE,
                user.getId(),
                user.getTenantId(),
                user.getRole(),
                correlationId);

        adminAuditService.record(
                user.getId(),
                user.getTenantId(),
                "ADMIN_TOKEN_REFRESH_SUCCESS",
                "REFRESH_TOKEN",
                savedReplacement.getId().toString(),
                correlationId,
                Map.of(
                        "familyId", current.getFamilyId().toString(),
                        "ipAddress", safe(ipAddress),
                        "userAgent", safe(truncate(userAgent, 512))));

        return new RefreshTokenResponse(
                accessToken,
                newRawToken,
                "Bearer",
                jwtTokenService.getExpiresInSeconds(),
                getRefreshExpiresInSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken, String correlationId) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshTokenRecord record = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (record == null) {
            incrementAuthCounter("traceguard.auth.logout.failed", FLOW_LOGOUT, OUTCOME_FAILURE, REASON_TOKEN_NOT_FOUND);
            log.warn(
                    "event={} outcome={} reasonCode={} correlationId={}",
                    EVENT_AUTH_LOGOUT_FAILURE,
                    OUTCOME_FAILURE,
                    REASON_TOKEN_NOT_FOUND,
                    correlationId);
            return;
        }

        Instant now = Instant.now();
        revokeFamily(record.getFamilyId(), now, "LOGOUT");

        adminAuditService.record(
                record.getUserId(),
                record.getTenantId(),
                "ADMIN_LOGOUT",
                "REFRESH_TOKEN",
                record.getId().toString(),
                correlationId,
                Map.of("familyId", record.getFamilyId().toString()));

        incrementAuthCounter("traceguard.auth.logout.success", FLOW_LOGOUT, OUTCOME_SUCCESS, REASON_NONE);
        log.info(
                "event={} outcome={} reasonCode={} userId={} tenantId={} correlationId={}",
                EVENT_AUTH_LOGOUT_SUCCESS,
                OUTCOME_SUCCESS,
                REASON_NONE,
                record.getUserId(),
                record.getTenantId(),
                correlationId);
    }

    public long getRefreshExpiresInSeconds() {
        return Duration.ofDays(refreshTokenDays).getSeconds();
    }

    private void revokeFamily(UUID familyId, Instant now, String reason) {
        List<RefreshTokenRecord> family = refreshTokenRepository.findAllByFamilyId(familyId);
        for (RefreshTokenRecord token : family) {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
            if (token.getRevocationReason() == null) {
                token.setRevocationReason(reason);
            }
        }
        refreshTokenRepository.saveAll(family);
    }

    private void revokeToken(RefreshTokenRecord token, String reason, Instant now) {
        token.setRevokedAt(now);
        token.setRevocationReason(reason);
        refreshTokenRepository.save(token);
    }

    private ResponseStatusException unauthorized(String message, String reasonCode) {
        incrementAuthCounter("traceguard.auth.refresh.failed", FLOW_REFRESH, OUTCOME_FAILURE, reasonCode);
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private void incrementAuthCounter(String metricName, String flow, String outcome, String reasonCode) {
        meterRegistry.counter(
                metricName,
                "flow",
                flow,
                "outcome",
                outcome,
                "reason",
                reasonCode).increment();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash refresh token", ex);
        }
    }

    private String prefixOf(String rawToken) {
        return rawToken.length() <= 12 ? rawToken : rawToken.substring(0, 12);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record IssuedRefreshToken(String rawToken, RefreshTokenRecord record) {
    }
}
