package com.traceguard.adminaudit;

import com.traceguard.adminaudit.dto.AdminAuditEventResponse;
import com.traceguard.adminaudit.dto.AdminAuditQueryRequest;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/admin/audit-events")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public ApiSuccessResponse<List<AdminAuditEventResponse>> list(
            @Valid AdminAuditQueryRequest query,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        JwtPrincipal actor = requirePrincipal(authentication);
        Page<AdminAuditEventResponse> result = adminAuditService.list(query, actor);

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
                ));
    }

    private JwtPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }
}
