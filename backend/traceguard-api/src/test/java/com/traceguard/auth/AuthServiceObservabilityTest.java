package com.traceguard.auth;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.dto.LoginRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceObservabilityTest {

    private UserAccountRepository userRepository;
    private JwtTokenService jwtTokenService;
    private RefreshTokenService refreshTokenService;
    private AdminAuditService adminAuditService;
    private SimpleMeterRegistry meterRegistry;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserAccountRepository.class);
        jwtTokenService = mock(JwtTokenService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        adminAuditService = mock(AdminAuditService.class);
        meterRegistry = new SimpleMeterRegistry();

        authService = new AuthService(
                userRepository,
                org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance(),
                jwtTokenService,
                refreshTokenService,
                new com.traceguard.common.sanitization.InputSanitizer(),
                adminAuditService,
                meterRegistry);
    }

    @Test
    void loginSuccessIncrementsLoginSuccessMetric() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UserAccount user = activeUser(userId, tenantId, AdminRole.SUPER_ADMIN);

        when(userRepository.findByEmailIgnoreCase("admin@traceguard.local")).thenReturn(Optional.of(user));
        when(jwtTokenService.issueAccessToken(user)).thenReturn("jwt-token");
        when(jwtTokenService.getExpiresInSeconds()).thenReturn(900L);
        when(refreshTokenService.issueForLogin(eq(user), anyString(), anyString(), anyString()))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken(
                        "refresh-token",
                        refreshRecord(userId, tenantId)));
        when(refreshTokenService.getRefreshExpiresInSeconds()).thenReturn(604800L);

        authService.login(
                new LoginRequest("admin@traceguard.local", "password"),
                "corr-login",
                "127.0.0.1",
                "JUnit");

        assertThat(meterRegistry.get("traceguard.auth.login.success")
                .tag("flow", "login")
                .tag("outcome", "success")
                .tag("reason", "none")
                .counter()
                .count()).isEqualTo(1.0d);
        verify(adminAuditService).record(eq(userId), eq(tenantId), eq("ADMIN_LOGIN_SUCCESS"),
                eq("USER_ACCOUNT"), anyString(), eq("corr-login"), anyMap());
    }

    @Test
    void loginFailureIncrementsLoginFailedMetric() {
        when(userRepository.findByEmailIgnoreCase("admin@traceguard.local")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(
                        new LoginRequest("admin@traceguard.local", "password"),
                        "corr-login-fail",
                        "127.0.0.1",
                        "JUnit"));

        assertThat(exception.getStatusCode().value()).isEqualTo(401);
        assertThat(meterRegistry.get("traceguard.auth.login.failed")
                .tag("flow", "login")
                .tag("outcome", "failure")
                .tag("reason", "invalid_credentials")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    private UserAccount activeUser(UUID userId, UUID tenantId, AdminRole role) throws Exception {
        UserAccount user = new UserAccount();
        setPrivateField(user, "id", userId);
        user.setTenantId(tenantId);
        user.setEmail("admin@traceguard.local");
        user.setPasswordHash("password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private RefreshTokenRecord refreshRecord(UUID userId, UUID tenantId) throws Exception {
        RefreshTokenRecord record = new RefreshTokenRecord();
        setPrivateField(record, "id", UUID.randomUUID());
        record.setUserId(userId);
        record.setTenantId(tenantId);
        record.setFamilyId(UUID.randomUUID());
        record.setTokenHash("hash");
        record.setTokenPrefix("prefix");
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        return record;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
