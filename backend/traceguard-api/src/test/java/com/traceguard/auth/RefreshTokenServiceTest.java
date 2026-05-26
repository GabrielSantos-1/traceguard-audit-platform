package com.traceguard.auth;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.dto.RefreshTokenResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

        private RefreshTokenRepository refreshTokenRepository;
        private UserAccountRepository userAccountRepository;
        private JwtTokenService jwtTokenService;
        private AdminAuditService adminAuditService;
        private RefreshTokenService refreshTokenService;
        private SimpleMeterRegistry meterRegistry;

        @BeforeEach
        void setUp() {
                refreshTokenRepository = mock(RefreshTokenRepository.class);
                userAccountRepository = mock(UserAccountRepository.class);
                jwtTokenService = mock(JwtTokenService.class);
                adminAuditService = mock(AdminAuditService.class);
                meterRegistry = new SimpleMeterRegistry();

                refreshTokenService = new RefreshTokenService(
                                refreshTokenRepository,
                                userAccountRepository,
                                jwtTokenService,
                                adminAuditService,
                                meterRegistry,
                                7);
        }

        @Test
        void issueForLogin_deve_persistir_hash_e_nao_token_bruto() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();

                UserAccount user = activeUser(userId, tenantId, AdminRole.TENANT_ADMIN);

                when(refreshTokenRepository.save(any(RefreshTokenRecord.class)))
                                .thenAnswer(invocation -> {
                                        RefreshTokenRecord record = invocation.getArgument(0);
                                        setPrivateField(record, "id", UUID.randomUUID());
                                        return record;
                                });

                RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issueForLogin(
                                user,
                                "corr-issue",
                                "127.0.0.1",
                                "JUnit");

                assertThat(issued.rawToken()).isNotBlank();
                assertThat(issued.record()).isNotNull();
                assertThat(issued.record().getTokenHash()).isNotBlank();
                assertThat(issued.record().getTokenHash()).isNotEqualTo(issued.rawToken());
                assertThat(issued.record().getTokenPrefix()).isNotBlank();
                assertThat(issued.record().getUserId()).isEqualTo(userId);
                assertThat(issued.record().getTenantId()).isEqualTo(tenantId);
                assertThat(issued.record().getFamilyId()).isNotNull();
                assertThat(issued.record().getExpiresAt()).isAfter(Instant.now());

                verify(refreshTokenRepository).save(any(RefreshTokenRecord.class));
                verify(adminAuditService).record(
                                eq(userId),
                                eq(tenantId),
                                eq("ADMIN_REFRESH_TOKEN_ISSUED"),
                                eq("REFRESH_TOKEN"),
                                anyString(),
                                eq("corr-issue"),
                                anyMap());
        }

        @Test
        void refresh_deve_rotacionar_token() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();
                UUID familyId = UUID.randomUUID();

                UserAccount user = activeUser(userId, tenantId, AdminRole.TENANT_ADMIN);

                RefreshTokenRecord current = new RefreshTokenRecord();
                setPrivateField(current, "id", UUID.randomUUID());
                current.setUserId(userId);
                current.setTenantId(tenantId);
                current.setFamilyId(familyId);
                current.setTokenHash(hashForTest("raw-refresh-token"));
                current.setTokenPrefix("raw-refresh");
                current.setExpiresAt(Instant.now().plusSeconds(3600));

                when(refreshTokenRepository.findByTokenHash(hashForTest("raw-refresh-token")))
                                .thenReturn(Optional.of(current));
                when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
                when(jwtTokenService.issueAccessToken(user)).thenReturn("new-access-token");
                when(jwtTokenService.getExpiresInSeconds()).thenReturn(900L);
                when(refreshTokenRepository.save(any(RefreshTokenRecord.class)))
                                .thenAnswer(invocation -> {
                                        RefreshTokenRecord record = invocation.getArgument(0);
                                        if (record.getId() == null) {
                                                setPrivateField(record, "id", UUID.randomUUID());
                                        }
                                        return record;
                                });

                RefreshTokenResponse response = refreshTokenService.refresh(
                                "raw-refresh-token",
                                "corr-refresh",
                                "127.0.0.1",
                                "JUnit");

                assertThat(response.accessToken()).isEqualTo("new-access-token");
                assertThat(response.refreshToken()).isNotBlank();
                assertThat(response.refreshToken()).isNotEqualTo("raw-refresh-token");
                assertThat(response.tokenType()).isEqualTo("Bearer");
                assertThat(response.expiresIn()).isEqualTo(900L);
                assertThat(response.refreshExpiresIn()).isGreaterThan(0L);

                assertThat(current.getLastUsedAt()).isNotNull();
                assertThat(current.getRevokedAt()).isNotNull();
                assertThat(current.getRevocationReason()).isEqualTo("ROTATED");
                assertThat(current.getReplacedByTokenId()).isNotNull();

                verify(refreshTokenRepository, atLeast(2)).save(any(RefreshTokenRecord.class));
                verify(adminAuditService).record(
                                eq(userId),
                                eq(tenantId),
                                eq("ADMIN_TOKEN_REFRESH_SUCCESS"),
                                eq("REFRESH_TOKEN"),
                                anyString(),
                                eq("corr-refresh"),
                                anyMap());
        }

        @Test
        void refresh_com_token_rotacionado_deve_detectar_reuse_e_revogar_familia() throws Exception {
                UUID familyId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();

                RefreshTokenRecord reused = new RefreshTokenRecord();
                setPrivateField(reused, "id", UUID.randomUUID());
                reused.setUserId(userId);
                reused.setTenantId(tenantId);
                reused.setFamilyId(familyId);
                reused.setTokenHash(hashForTest("reused-token"));
                reused.setTokenPrefix("reused");
                reused.setExpiresAt(Instant.now().plusSeconds(3600));
                reused.setRevokedAt(Instant.now());
                reused.setReplacedByTokenId(UUID.randomUUID());

                when(refreshTokenRepository.findByTokenHash(hashForTest("reused-token")))
                                .thenReturn(Optional.of(reused));
                when(refreshTokenRepository.findAllByFamilyId(familyId)).thenReturn(List.of(reused));
                when(refreshTokenRepository.save(any(RefreshTokenRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(refreshTokenRepository.saveAll(anyList()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResponseStatusException ex = assertThrows(
                                ResponseStatusException.class,
                                () -> refreshTokenService.refresh("reused-token", "corr-reuse", "127.0.0.1", "JUnit"));

                assertThat(ex.getStatusCode().value()).isEqualTo(401);
                assertThat(reused.getReuseDetectedAt()).isNotNull();
                assertThat(meterRegistry.get("traceguard.auth.refresh.reuse_detected")
                                .tag("flow", "refresh")
                                .tag("outcome", "failure")
                                .tag("reason", "reuse_detected")
                                .counter()
                                .count()).isEqualTo(1.0d);
                assertThat(meterRegistry.get("traceguard.auth.refresh.failed")
                                .tag("flow", "refresh")
                                .tag("outcome", "failure")
                                .tag("reason", "reuse_detected")
                                .counter()
                                .count()).isEqualTo(1.0d);

                verify(refreshTokenRepository).save(reused);
                verify(refreshTokenRepository).saveAll(anyList());
                verify(adminAuditService).record(
                                eq(userId),
                                eq(tenantId),
                                eq("ADMIN_REFRESH_TOKEN_REUSE_DETECTED"),
                                eq("REFRESH_TOKEN"),
                                anyString(),
                                eq("corr-reuse"),
                                anyMap());
        }

        @Test
        void refresh_com_token_expirado_deve_falhar() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();
                UUID familyId = UUID.randomUUID();

                RefreshTokenRecord expired = new RefreshTokenRecord();
                setPrivateField(expired, "id", UUID.randomUUID());
                expired.setUserId(userId);
                expired.setTenantId(tenantId);
                expired.setFamilyId(familyId);
                expired.setTokenHash(hashForTest("expired-token"));
                expired.setTokenPrefix("expired");
                expired.setExpiresAt(Instant.now().minusSeconds(60));

                when(refreshTokenRepository.findByTokenHash(hashForTest("expired-token")))
                                .thenReturn(Optional.of(expired));
                when(refreshTokenRepository.save(any(RefreshTokenRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResponseStatusException ex = assertThrows(
                                ResponseStatusException.class,
                                () -> refreshTokenService.refresh("expired-token", "corr-expired", "127.0.0.1",
                                                "JUnit"));

                assertThat(ex.getStatusCode().value()).isEqualTo(401);
                assertThat(expired.getRevokedAt()).isNotNull();
                assertThat(expired.getRevocationReason()).isEqualTo("EXPIRED");
                assertThat(meterRegistry.get("traceguard.auth.refresh.failed")
                                .tag("flow", "refresh")
                                .tag("outcome", "failure")
                                .tag("reason", "expired_token")
                                .counter()
                                .count()).isEqualTo(1.0d);
        }

        @Test
        void refresh_com_usuario_inativo_deve_falhar() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();
                UUID familyId = UUID.randomUUID();

                RefreshTokenRecord current = new RefreshTokenRecord();
                setPrivateField(current, "id", UUID.randomUUID());
                current.setUserId(userId);
                current.setTenantId(tenantId);
                current.setFamilyId(familyId);
                current.setTokenHash(hashForTest("inactive-user-token"));
                current.setTokenPrefix("inactive");
                current.setExpiresAt(Instant.now().plusSeconds(3600));

                UserAccount inactiveUser = activeUser(userId, tenantId, AdminRole.TENANT_ADMIN);
                inactiveUser.setStatus(UserStatus.DISABLED);

                when(refreshTokenRepository.findByTokenHash(hashForTest("inactive-user-token")))
                                .thenReturn(Optional.of(current));
                when(userAccountRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

                ResponseStatusException ex = assertThrows(
                                ResponseStatusException.class,
                                () -> refreshTokenService.refresh("inactive-user-token", "corr-inactive", "127.0.0.1",
                                                "JUnit"));

                assertThat(ex.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        void logout_deve_revogar_familia() throws Exception {
                UUID familyId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();

                RefreshTokenRecord token1 = new RefreshTokenRecord();
                setPrivateField(token1, "id", UUID.randomUUID());
                token1.setUserId(userId);
                token1.setTenantId(tenantId);
                token1.setFamilyId(familyId);
                token1.setTokenHash(hashForTest("logout-token"));
                token1.setTokenPrefix("logout");
                token1.setExpiresAt(Instant.now().plusSeconds(3600));

                RefreshTokenRecord token2 = new RefreshTokenRecord();
                setPrivateField(token2, "id", UUID.randomUUID());
                token2.setUserId(userId);
                token2.setTenantId(tenantId);
                token2.setFamilyId(familyId);
                token2.setTokenHash("other-hash");
                token2.setTokenPrefix("other");
                token2.setExpiresAt(Instant.now().plusSeconds(3600));

                when(refreshTokenRepository.findByTokenHash(hashForTest("logout-token")))
                                .thenReturn(Optional.of(token1));
                when(refreshTokenRepository.findAllByFamilyId(familyId)).thenReturn(List.of(token1, token2));
                when(refreshTokenRepository.saveAll(anyList()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                refreshTokenService.logout("logout-token", "corr-logout");

                assertThat(token1.getRevokedAt()).isNotNull();
                assertThat(token2.getRevokedAt()).isNotNull();
                assertThat(token1.getRevocationReason()).isEqualTo("LOGOUT");
                assertThat(token2.getRevocationReason()).isEqualTo("LOGOUT");
                assertThat(meterRegistry.get("traceguard.auth.logout.success")
                                .tag("flow", "logout")
                                .tag("outcome", "success")
                                .tag("reason", "none")
                                .counter()
                                .count()).isEqualTo(1.0d);

                verify(refreshTokenRepository).saveAll(anyList());
                verify(adminAuditService).record(
                                eq(userId),
                                eq(tenantId),
                                eq("ADMIN_LOGOUT"),
                                eq("REFRESH_TOKEN"),
                                anyString(),
                                eq("corr-logout"),
                                anyMap());
        }

        @Test
        void logout_com_token_desconhecido_deve_registrar_falha() throws Exception {
                when(refreshTokenRepository.findByTokenHash(hashForTest("missing-token"))).thenReturn(Optional.empty());

                refreshTokenService.logout("missing-token", "corr-logout-fail");

                assertThat(meterRegistry.get("traceguard.auth.logout.failed")
                                .tag("flow", "logout")
                                .tag("outcome", "failure")
                                .tag("reason", "token_not_found")
                                .counter()
                                .count()).isEqualTo(1.0d);
                verify(adminAuditService, never()).record(any(), any(), anyString(), anyString(), anyString(), anyString(),
                                anyMap());
        }

        private UserAccount activeUser(UUID userId, UUID tenantId, AdminRole role) throws Exception {
                UserAccount user = new UserAccount();
                setPrivateField(user, "id", userId);
                user.setTenantId(tenantId);
                user.setEmail("admin@test.com");
                user.setPasswordHash("hash");
                user.setRole(role);
                user.setStatus(UserStatus.ACTIVE);
                return user;
        }

        private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
        }

        private String hashForTest(String rawToken) throws Exception {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return java.util.HexFormat.of().formatHex(hashed);
        }
}
