package com.traceguard.monitoring.activity;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.monitoring.activity.dto.ActivityQueryRequest;
import com.traceguard.monitoring.activity.dto.ActivityResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserActivityControllerTest {

    private UserActivityController controller;
    private UserActivityService service;
    private Authentication authentication;
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        service = mock(UserActivityService.class);
        controller = new UserActivityController(service);
        authentication = mock(Authentication.class);
        httpRequest = mock(HttpServletRequest.class);
    }

    @Test
    void listReturnsPagedResponse() {
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.SUPER_ADMIN);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(httpRequest.getAttribute(CorrelationIdUtil.ATTRIBUTE_NAME)).thenReturn("test-corr");

        ActivityResponse response = new ActivityResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SUPER_ADMIN", "READ", "admin/test", null,
                "GET", "/api/v1/admin/test", 200,
                "127.0.0.1", "corr-1", true,
                null, null, null, 15L, Instant.now());

        when(service.list(any(ActivityQueryRequest.class), any(JwtPrincipal.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        ApiSuccessResponse<List<ActivityResponse>> result = controller.list(
                new ActivityQueryRequest(), authentication, httpRequest);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).action()).isEqualTo("READ");
    }

    @Test
    void listRequiresAuthentication() {
        when(authentication.getPrincipal()).thenReturn("anonymous");

        assertThrows(ResponseStatusException.class,
                () -> controller.list(new ActivityQueryRequest(), authentication, httpRequest));
    }

    @Test
    void listRequiresNonNullAuthentication() {
        assertThrows(ResponseStatusException.class,
                () -> controller.list(new ActivityQueryRequest(), null, httpRequest));
    }
}
