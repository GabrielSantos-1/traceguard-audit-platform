package com.traceguard.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issuesAndParsesJwtWithExpectedClaims() {
        JwtTokenService service = buildService("0123456789abcdef0123456789abcdef", "traceguard-test", "traceguard-admin",
                15L, new SimpleMeterRegistry());

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        UserAccount user = new UserAccount();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setTenantId(tenantId);
        user.setEmail("admin@traceguard.local");
        user.setPasswordHash("$2a$10$dummy");
        user.setRole(AdminRole.SUPER_ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        String token = service.issueAccessToken(user);
        assertNotNull(token);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("traceguard-test", claims.getIssuer());
        assertEquals("traceguard-admin", claims.getAudience().iterator().next());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
        assertEquals(tenantId.toString(), claims.get("tenantId", String.class));
        assertEquals(AdminRole.SUPER_ADMIN.name(), claims.get("role", String.class));

        JwtPrincipal principal = service.parse(token);
        assertEquals(userId, principal.userId());
        assertEquals(tenantId, principal.tenantId());
        assertEquals(AdminRole.SUPER_ADMIN, principal.role());
    }

    @Test
    void issuesTokenWithUniqueJti() {
        JwtTokenService service = buildService("0123456789abcdef0123456789abcdef", "traceguard-test", "traceguard-admin",
                15L, new SimpleMeterRegistry());
        UserAccount user = user(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);

        String token1 = service.issueAccessToken(user);
        String token2 = service.issueAccessToken(user);

        Claims claims1 = Jwts.parser().verifyWith(Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes()))
                .build().parseSignedClaims(token1).getPayload();
        Claims claims2 = Jwts.parser().verifyWith(Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes()))
                .build().parseSignedClaims(token2).getPayload();
        assertThat(claims1.getId()).isNotBlank();
        assertThat(claims2.getId()).isNotBlank();
        assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
    }

    @Test
    void rejectsExpiredTokenAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenService service = buildService(secret, "traceguard-test", "traceguard-admin", 15L, meterRegistry);

        String expiredToken = token(secret, "traceguard-test", "traceguard-admin", UUID.randomUUID(), UUID.randomUUID(),
                AdminRole.SUPER_ADMIN.name(), Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), true, true,
                true);

        assertThrows(ResponseStatusException.class, () -> service.parse(expiredToken, "corr-expired"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.expired").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsTokenWithInvalidSignatureAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        JwtTokenService service = buildService("0123456789abcdef0123456789abcdef", "traceguard-test", "traceguard-admin",
                15L, meterRegistry);

        String tokenWithOtherSecret = token("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "traceguard-test", "traceguard-admin",
                UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN.name(), Instant.now(),
                Instant.now().plusSeconds(900), true, true, true);

        assertThrows(ResponseStatusException.class, () -> service.parse(tokenWithOtherSecret, "corr-signature"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.invalid_signature").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsTokenWithInvalidIssuerAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenService service = buildService(secret, "traceguard-test", "traceguard-admin", 15L, meterRegistry);

        String token = token(secret, "other-issuer", "traceguard-admin", UUID.randomUUID(), UUID.randomUUID(),
                AdminRole.SUPER_ADMIN.name(), Instant.now(), Instant.now().plusSeconds(900), true, true, true);

        assertThrows(ResponseStatusException.class, () -> service.parse(token, "corr-issuer"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.invalid_issuer").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsTokenWithInvalidAudienceAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenService service = buildService(secret, "traceguard-test", "traceguard-admin", 15L, meterRegistry);

        String token = token(secret, "traceguard-test", "other-audience", UUID.randomUUID(), UUID.randomUUID(),
                AdminRole.SUPER_ADMIN.name(), Instant.now(), Instant.now().plusSeconds(900), true, true, true);

        assertThrows(ResponseStatusException.class, () -> service.parse(token, "corr-audience"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.invalid_audience").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsTokenWithoutRequiredClaimAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenService service = buildService(secret, "traceguard-test", "traceguard-admin", 15L, meterRegistry);

        String tokenWithoutTenant = token(secret, "traceguard-test", "traceguard-admin", UUID.randomUUID(),
                UUID.randomUUID(), AdminRole.SUPER_ADMIN.name(), Instant.now(), Instant.now().plusSeconds(900), false,
                true, true);

        assertThrows(ResponseStatusException.class, () -> service.parse(tokenWithoutTenant, "corr-missing-claim"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.missing_claim").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsTokenWithoutJtiAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        String secret = "0123456789abcdef0123456789abcdef";
        JwtTokenService service = buildService(secret, "traceguard-test", "traceguard-admin", 15L, meterRegistry);

        String tokenWithoutJti = token(secret, "traceguard-test", "traceguard-admin", UUID.randomUUID(),
                UUID.randomUUID(), AdminRole.SUPER_ADMIN.name(), Instant.now(), Instant.now().plusSeconds(900), true,
                true, false);

        assertThrows(ResponseStatusException.class, () -> service.parse(tokenWithoutJti, "corr-missing-jti"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.missing_claim").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void rejectsMalformedTokenAndIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        JwtTokenService service = buildService("0123456789abcdef0123456789abcdef", "traceguard-test", "traceguard-admin",
                15L, meterRegistry);

        assertThrows(ResponseStatusException.class, () -> service.parse("not-a-jwt", "corr-malformed"));
        assertThat(meterRegistry.get("traceguard.auth.jwt.malformed").counter().count()).isEqualTo(1.0d);
    }

    @Test
    void failsWhenJwtConfigurationIsWeakOrMissing() {
        JwtTokenService missingSecret = new JwtTokenService(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(missingSecret, "secret", "");
        ReflectionTestUtils.setField(missingSecret, "issuer", "traceguard-test");
        ReflectionTestUtils.setField(missingSecret, "audience", "traceguard-admin");
        ReflectionTestUtils.setField(missingSecret, "accessTokenMinutes", 15L);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(missingSecret, "init"));

        JwtTokenService missingAudience = new JwtTokenService(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(missingAudience, "secret", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(missingAudience, "issuer", "traceguard-test");
        ReflectionTestUtils.setField(missingAudience, "audience", "");
        ReflectionTestUtils.setField(missingAudience, "accessTokenMinutes", 15L);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(missingAudience, "init"));

        JwtTokenService weakSecret = new JwtTokenService(new SimpleMeterRegistry());
        ReflectionTestUtils.setField(weakSecret, "secret", "short-secret");
        ReflectionTestUtils.setField(weakSecret, "issuer", "traceguard-test");
        ReflectionTestUtils.setField(weakSecret, "audience", "traceguard-admin");
        ReflectionTestUtils.setField(weakSecret, "accessTokenMinutes", 15L);
        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(weakSecret, "init"));
    }

    private JwtTokenService buildService(
            String secret,
            String issuer,
            String audience,
            long accessTokenMinutes,
            SimpleMeterRegistry meterRegistry) {
        JwtTokenService service = new JwtTokenService(meterRegistry);
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "issuer", issuer);
        ReflectionTestUtils.setField(service, "audience", audience);
        ReflectionTestUtils.setField(service, "accessTokenMinutes", accessTokenMinutes);
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    private UserAccount user(UUID userId, UUID tenantId, AdminRole role) {
        UserAccount user = new UserAccount();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setTenantId(tenantId);
        user.setEmail("admin@traceguard.local");
        user.setPasswordHash("$2a$10$dummy");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private String token(
            String secret,
            String issuer,
            String audience,
            UUID userId,
            UUID tenantId,
            String role,
            Instant iat,
            Instant exp,
            boolean includeTenantId,
            boolean includeRole,
            boolean includeJti) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(iat))
                .expiration(Date.from(exp));
        if (includeJti) {
            builder.id(UUID.randomUUID().toString());
        }
        if (includeTenantId) {
            builder.claim("tenantId", tenantId.toString());
        }
        if (includeRole) {
            builder.claim("role", role);
        }
        return builder.signWith(key).compact();
    }
}
