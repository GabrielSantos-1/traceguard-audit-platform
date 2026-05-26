package com.traceguard.auth;

import com.traceguard.auth.dto.LoginResponse;
import com.traceguard.auth.dto.RefreshTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void loginReturns200WithExpectedContract() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LoginResponse response = new LoginResponse(
                "access-token-value",
                "refresh-token-value",
                "Bearer",
                900,
                604800,
                new LoginResponse.UserSummary(userId, tenantId, "SUPER_ADMIN"));

        when(authService.login(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@traceguard.local",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-value"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800))
                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.user.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.data.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.meta.correlationId").isNotEmpty());
    }

    @Test
    void refreshReturns200WithExpectedContract() throws Exception {
        when(refreshTokenService.refresh(eq("refresh-token-value"), any(), any(), any()))
                .thenReturn(new RefreshTokenResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "Bearer",
                        900,
                        604800));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token-value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.meta.correlationId").isNotEmpty());
    }

    @Test
    void logoutReturns204ForValidPayload() throws Exception {
        doNothing().when(refreshTokenService).logout(eq("refresh-token-value"), any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token-value"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginReturns400ForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty());
    }

    @Test
    void loginReturns401ForInvalidCredentialsWithoutLeakingInternals() throws Exception {
        doThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"))
                .when(authService).login(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@traceguard.local",
                                  "password": "WrongPass@123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Invalid credentials"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("stacktrace"))));
    }

    @Test
    void refreshReturns401ForReusedTokenWithoutEchoingTokenValue() throws Exception {
        String rawToken = "sensitive-refresh-token-value";
        doThrow(new ResponseStatusException(UNAUTHORIZED, "Refresh token reuse detected"))
                .when(refreshTokenService).refresh(eq(rawToken), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "sensitive-refresh-token-value"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Refresh token reuse detected"))
                .andExpect(content().string(not(containsString(rawToken))));
    }
}
