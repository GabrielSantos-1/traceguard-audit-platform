package com.traceguard.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.traceguard.auth.AuthObservability.*;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${traceguard.jwt.secret}")
    private String secret;

    @Value("${traceguard.jwt.issuer}")
    private String issuer;

    @Value("${traceguard.jwt.audience}")
    private String audience;

    @Value("${traceguard.jwt.access-token-minutes}")
    private long accessTokenMinutes;

    private final MeterRegistry meterRegistry;
    private SecretKey signingKey;

    public JwtTokenService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException("JWT secret is required and must be at least 32 characters");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT issuer is required");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException("JWT audience is required");
        }
        if (accessTokenMinutes <= 0) {
            throw new IllegalStateException("JWT access token duration must be greater than zero");
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(accessTokenMinutes));
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(jti)
                .claim("tenantId", user.getTenantId().toString())
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public long getExpiresInSeconds() {
        return Duration.ofMinutes(accessTokenMinutes).getSeconds();
    }

    public JwtPrincipal parse(String token) {
        return parse(token, null);
    }

    public JwtPrincipal parse(String token, String correlationId) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                throw new JwtValidationException(REASON_JWT_INVALID_AUDIENCE);
            }

            UUID userId = parseUuidClaim(claims.getSubject());
            UUID tenantId = parseUuidClaim(claims.get("tenantId", String.class));
            String roleClaim = claims.get("role", String.class);
            if (roleClaim == null || roleClaim.isBlank()) {
                throw missingClaimFailure();
            }
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw missingClaimFailure();
            }
            if (claims.getIssuedAt() == null || claims.getExpiration() == null) {
                throw missingClaimFailure();
            }
            AdminRole role = AdminRole.parse(roleClaim);
            return new JwtPrincipal(userId, tenantId, role);
        } catch (ExpiredJwtException ex) {
            throw jwtUnauthorized(REASON_JWT_EXPIRED, correlationId);
        } catch (IncorrectClaimException ex) {
            String reason = ex.getClaimName() != null && ex.getClaimName().equals("aud") ? REASON_JWT_INVALID_AUDIENCE
                    : REASON_JWT_INVALID_ISSUER;
            throw jwtUnauthorized(reason, correlationId);
        } catch (MissingClaimException ex) {
            throw jwtUnauthorized(REASON_JWT_MISSING_CLAIM, correlationId);
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            throw jwtUnauthorized(REASON_JWT_MALFORMED, correlationId);
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            throw jwtUnauthorized(REASON_JWT_INVALID_SIGNATURE, correlationId);
        } catch (JwtValidationException ex) {
            throw jwtUnauthorized(ex.reasonCode(), correlationId);
        } catch (Exception ex) {
            throw jwtUnauthorized(REASON_JWT_INVALID_CLAIM, correlationId);
        }
    }

    private UUID parseUuidClaim(String value) {
        if (value == null || value.isBlank()) {
            throw missingClaimFailure();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new JwtValidationException(REASON_JWT_INVALID_CLAIM);
        }
    }

    private JwtValidationException missingClaimFailure() {
        return new JwtValidationException(REASON_JWT_MISSING_CLAIM);
    }

    private ResponseStatusException jwtUnauthorized(String reasonCode, String correlationId) {
        meterRegistry.counter("traceguard.auth.jwt.validation.failed", "reason", reasonCode).increment();
        meterRegistry.counter(specificJwtMetric(reasonCode)).increment();
        log.warn(
                "event={} outcome={} reasonCode={} correlationId={}",
                EVENT_AUTH_JWT_VALIDATION_FAILURE,
                OUTCOME_FAILURE,
                reasonCode,
                correlationId == null ? "n/a" : correlationId);
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    private String specificJwtMetric(String reasonCode) {
        return switch (reasonCode) {
            case REASON_JWT_INVALID_SIGNATURE -> "traceguard.auth.jwt.invalid_signature";
            case REASON_JWT_EXPIRED -> "traceguard.auth.jwt.expired";
            case REASON_JWT_MALFORMED -> "traceguard.auth.jwt.malformed";
            case REASON_JWT_INVALID_ISSUER -> "traceguard.auth.jwt.invalid_issuer";
            case REASON_JWT_INVALID_AUDIENCE -> "traceguard.auth.jwt.invalid_audience";
            case REASON_JWT_MISSING_CLAIM -> "traceguard.auth.jwt.missing_claim";
            default -> "traceguard.auth.jwt.invalid_claim";
        };
    }

    private static final class JwtValidationException extends RuntimeException {
        private final String reasonCode;

        private JwtValidationException(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        private String reasonCode() {
            return reasonCode;
        }
    }
}
