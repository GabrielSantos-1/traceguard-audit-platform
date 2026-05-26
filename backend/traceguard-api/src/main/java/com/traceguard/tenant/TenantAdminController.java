package com.traceguard.tenant;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.tenant.dto.CreateTenantRequest;
import com.traceguard.tenant.dto.TenantResponse;
import com.traceguard.tenant.dto.UpdateTenantRequest;
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
@RequestMapping("/api/v1/admin/tenants")
public class TenantAdminController {

    private final TenantService tenantService;

    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiSuccessResponse<TenantResponse> create(
            @Valid @RequestBody CreateTenantRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal actor = requirePrincipal(authentication);
        if (actor.role() != AdminRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can create tenant");
        }
        TenantResponse response = tenantService.create(request, actor, CorrelationIdUtil.getOrCreate(httpRequest));
        return ApiSuccessResponse.of(response, ApiMeta.basic(CorrelationIdUtil.getOrCreate(httpRequest)));
    }

    @GetMapping
    public ApiSuccessResponse<List<TenantResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal actor = requirePrincipal(authentication);
        if (actor.role() != AdminRole.SUPER_ADMIN && actor.role() != AdminRole.AUDITOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role cannot list tenants");
        }
        Page<TenantResponse> result = tenantService.list(page, size);
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

    @PatchMapping("/{id}")
    public ApiSuccessResponse<TenantResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        JwtPrincipal actor = requirePrincipal(authentication);
        if (actor.role() != AdminRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can update tenant");
        }
        TenantResponse response = tenantService.update(id, request, actor, CorrelationIdUtil.getOrCreate(httpRequest));
        return ApiSuccessResponse.of(response, ApiMeta.basic(CorrelationIdUtil.getOrCreate(httpRequest)));
    }

    private JwtPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }
}
