package com.traceguard.apikey;

import com.traceguard.apikey.dto.ApiKeyListItemResponse;
import com.traceguard.apikey.dto.CreateApiKeyRequest;
import com.traceguard.apikey.dto.CreateApiKeyResponse;
import com.traceguard.apikey.dto.RevokeApiKeyResponse;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
public class ApiKeyAdminController {

    private final ApiKeyAdminService service;

    public ApiKeyAdminController(ApiKeyAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccessResponse<CreateApiKeyResponse> create(
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = requirePrincipal(authentication);
        CreateApiKeyResponse response = service.create(request, principal, CorrelationIdUtil.getOrCreate(httpRequest));
        return ApiSuccessResponse.of(response, ApiMeta.basic(CorrelationIdUtil.getOrCreate(httpRequest)));
    }

    @GetMapping
    public ApiSuccessResponse<List<ApiKeyListItemResponse>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = requirePrincipal(authentication);
        Page<ApiKeyListItemResponse> result = service.list(tenantId, page, size, principal);
        return ApiSuccessResponse.of(
                result.getContent(),
                ApiMeta.paged(
                        CorrelationIdUtil.getOrCreate(httpRequest),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.hasNext(),
                        result.hasPrevious()
                )
        );
    }

    @PatchMapping("/{id}/revoke")
    public ApiSuccessResponse<RevokeApiKeyResponse> revoke(
            @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal principal = requirePrincipal(authentication);
        RevokeApiKeyResponse response = service.revoke(id, principal, CorrelationIdUtil.getOrCreate(httpRequest));
        return ApiSuccessResponse.of(response, ApiMeta.basic(CorrelationIdUtil.getOrCreate(httpRequest)));
    }

    private JwtPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }
}
