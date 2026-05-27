package com.traceguard.monitoring.activity;

import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.monitoring.activity.dto.ActivityQueryRequest;
import com.traceguard.monitoring.activity.dto.ActivityResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/activity")
public class UserActivityController {

    private final UserActivityService activityService;

    public UserActivityController(UserActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiSuccessResponse<List<ActivityResponse>> list(
            @Valid ActivityQueryRequest query,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        var actor = requirePrincipal(authentication);
        Page<ActivityResponse> result = activityService.list(query, actor);

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

    private com.traceguard.auth.JwtPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof com.traceguard.auth.JwtPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return principal;
    }
}
