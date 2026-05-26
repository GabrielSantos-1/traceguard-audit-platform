package com.traceguard.auth;

import com.traceguard.auth.dto.ChangePasswordRequest;
import com.traceguard.auth.dto.LoginRequest;
import com.traceguard.auth.dto.LoginResponse;
import com.traceguard.auth.dto.LogoutRequest;
import com.traceguard.auth.dto.RefreshTokenRequest;
import com.traceguard.auth.dto.RefreshTokenResponse;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ApiSuccessResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String correlationId = CorrelationIdUtil.getOrCreate(http);
        LoginResponse response = authService.login(
                request,
                correlationId,
                http.getRemoteAddr(),
                http.getHeader("User-Agent"));
        return ApiSuccessResponse.of(response, ApiMeta.basic(correlationId));
    }

    @PostMapping("/refresh")
    public ApiSuccessResponse<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest http) {
        String correlationId = CorrelationIdUtil.getOrCreate(http);
        RefreshTokenResponse response = refreshTokenService.refresh(
                request.refreshToken(),
                correlationId,
                http.getRemoteAddr(),
                http.getHeader("User-Agent"));
        return ApiSuccessResponse.of(response, ApiMeta.basic(correlationId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest http) {
        String correlationId = CorrelationIdUtil.getOrCreate(http);
        refreshTokenService.logout(request.refreshToken(), correlationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password") // Deprecated: use /api/v1/admin/account/password
    public ApiSuccessResponse<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        authService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        return ApiSuccessResponse.of(Map.of("message", "Password changed successfully"),
                ApiMeta.basic(""));
    }
}