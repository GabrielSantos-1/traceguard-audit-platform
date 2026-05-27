package com.traceguard.monitoring.detection;

import com.traceguard.monitoring.detection.dto.AlertQueryRequest;
import com.traceguard.monitoring.detection.dto.AlertResponse;
import com.traceguard.monitoring.detection.dto.BlockedIpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DetectionController.class)
@Import(com.traceguard.common.api.ApiExceptionHandler.class)
class DetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DetectionService detectionService;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listAlertsReturns200() throws Exception {
        UUID alertId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AlertResponse alert = new AlertResponse(
                alertId, "test-rule", "HIGH", "Test alert", null,
                tenantId, null, null, null, false, null, null, Instant.now()
        );

        when(detectionService.listAlerts(any(AlertQueryRequest.class), any(), anyBoolean()))
                .thenReturn(new PageImpl<>(List.of(alert)));

        mockMvc.perform(get("/api/v1/admin/detection/alerts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ruleName").value("test-rule"))
                .andExpect(jsonPath("$.data[0].severity").value("HIGH"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void acknowledgeAlertReturns204() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/detection/alerts/{id}/acknowledge", UUID.randomUUID())
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listBlockedIpsReturns200() throws Exception {
        BlockedIpResponse blocked = new BlockedIpResponse(
                UUID.randomUUID(), "192.168.1.1", UUID.randomUUID(),
                "brute-force", "test", Instant.now(), Instant.now().plusSeconds(3600), true
        );

        when(detectionService.listBlockedIps(any())).thenReturn(List.of(blocked));

        mockMvc.perform(get("/api/v1/admin/detection/blocked-ips")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ip").value("192.168.1.1"));
    }

    @Test
    void listAlertsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/detection/alerts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
