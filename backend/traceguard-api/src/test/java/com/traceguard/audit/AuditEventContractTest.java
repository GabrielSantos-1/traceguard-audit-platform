package com.traceguard.audit;

import com.traceguard.common.api.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditEventContractTest {

    private MockMvc mockMvc;
    @BeforeEach
    void setup() {
        AuditEventService service = mock(AuditEventService.class);
        when(service.create(any(), anyString(), anyString())).thenReturn(UUID.randomUUID());
        AuditEventController controller = new AuditEventController(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void postInvalidRequestReturnsStandardErrorPayload() throws Exception {
        mockMvc.perform(post("/api/v1/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/audit/events"))
                .andExpect(jsonPath("$.error.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.error.timestamp").isNotEmpty());
    }

    @Test
    void getInvalidPageSizeReturnsBadRequestContractError() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/audit/events"));
    }

    @Test
    void getInvalidSortFieldReturnsBadRequestContractError() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .param("sortBy", "tenantId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.path").value("/api/v1/audit/events"));
    }
}
