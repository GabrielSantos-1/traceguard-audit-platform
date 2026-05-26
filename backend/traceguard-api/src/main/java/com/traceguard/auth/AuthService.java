package com.traceguard.auth;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.dto.LoginRequest;
import com.traceguard.auth.dto.LoginResponse;
import com.traceguard.common.sanitization.InputSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static com.traceguard.auth.AuthObservability.*;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final InputSanitizer sanitizer;
    private final AdminAuditService adminAuditService;
    private final MeterRegistry meterRegistry;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            InputSanitizer sanitizer,
            AdminAuditService adminAuditService,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.sanitizer = sanitizer;
        this.adminAuditService = adminAuditService;
        this.meterRegistry = meterRegistry;
    }

    public LoginResponse login(LoginRequest request, String correlationId, String ipAddress, String userAgent) {
        String email = sanitizer.sanitizeText(request.email(), 150, "email").toLowerCase();
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn(
                            "event={} outcome={} reasonCode={} correlationId={}",
                            EVENT_AUTH_LOGIN_FAILURE,
                            OUTCOME_FAILURE,
                            REASON_INVALID_CREDENTIALS,
                            correlationId);
                    incrementAuthCounter("traceguard.auth.login.failed", FLOW_LOGIN, OUTCOME_FAILURE,
                            REASON_INVALID_CREDENTIALS);
                    return new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn(
                    "event={} outcome={} reasonCode={} userId={} tenantId={} role={} correlationId={}",
                    EVENT_AUTH_LOGIN_FAILURE,
                    OUTCOME_FAILURE,
                    REASON_INVALID_CREDENTIALS,
                    user.getId(),
                    user.getTenantId(),
                    user.getRole(),
                    correlationId);
            incrementAuthCounter("traceguard.auth.login.failed", FLOW_LOGIN, OUTCOME_FAILURE,
                    REASON_INVALID_CREDENTIALS);
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtTokenService.issueAccessToken(user);
        RefreshTokenService.IssuedRefreshToken issuedRefreshToken = refreshTokenService.issueForLogin(user,
                correlationId, ipAddress, userAgent);

        log.info(
                "event={} outcome={} reasonCode={} userId={} tenantId={} role={} correlationId={}",
                EVENT_AUTH_LOGIN_SUCCESS,
                OUTCOME_SUCCESS,
                REASON_NONE,
                user.getId(),
                user.getTenantId(),
                user.getRole(),
                correlationId);
        incrementAuthCounter("traceguard.auth.login.success", FLOW_LOGIN, OUTCOME_SUCCESS, REASON_NONE);

        adminAuditService.record(
                user.getId(),
                user.getTenantId(),
                "ADMIN_LOGIN_SUCCESS",
                "USER_ACCOUNT",
                user.getId().toString(),
                correlationId,
                Map.of("role", user.getRole().name()));

        return new LoginResponse(
                accessToken,
                issuedRefreshToken.rawToken(),
                "Bearer",
                jwtTokenService.getExpiresInSeconds(),
                refreshTokenService.getRefreshExpiresInSeconds(),
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getTenantId(),
                        user.getRole().name()));
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
}
