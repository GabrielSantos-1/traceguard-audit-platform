package com.traceguard.auth;

import com.traceguard.auth.dto.ChangePasswordRequest;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/account")
public class AdminAccountController {

    private final AuthService authService;

    public AdminAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/password")
    public ApiSuccessResponse<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        authService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        return ApiSuccessResponse.of(Map.of("message", "Password changed successfully"),
                ApiMeta.basic(""));
    }
}
