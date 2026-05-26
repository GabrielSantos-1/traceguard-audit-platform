package com.traceguard.tenant;

import com.traceguard.auth.AdminRole;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.tenant.dto.CreateTenantRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TenantAdminControllerAuthTest {

    @Test
    void rejectsCreateWithoutJwtPrincipal() {
        TenantService service = mock(TenantService.class);
        TenantAdminController controller = new TenantAdminController(service);
        HttpServletRequest request = new MockHttpServletRequest();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(new CreateTenantRequest("Tenant A", "tenant-a", TenantStatus.ACTIVE), null, request)
        );
        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void rejectsCreateWhenRoleIsNotSuperAdmin() {
        TenantService service = mock(TenantService.class);
        TenantAdminController controller = new TenantAdminController(service);
        HttpServletRequest request = new MockHttpServletRequest();
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), UUID.randomUUID(), AdminRole.OPERATOR);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(new CreateTenantRequest("Tenant A", "tenant-a", TenantStatus.ACTIVE), authentication, request)
        );
        assertEquals(403, exception.getStatusCode().value());
    }
}
