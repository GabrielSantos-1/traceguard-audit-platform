package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.monitoring.detection.dto.AlertQueryRequest;
import com.traceguard.monitoring.detection.dto.AlertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    @Mock
    private DetectionAlertRepository alertRepository;

    @Mock
    private BlockedIpRepository blockedIpRepository;

    private DetectionService detectionService;
    private final UUID tenantId = UUID.randomUUID();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        detectionService = new DetectionService(alertRepository, blockedIpRepository);
    }

    @Test
    void listAlertsReturnsPagedResults() {
        DetectionAlert alert = createAlert();
        when(alertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));

        AlertQueryRequest request = new AlertQueryRequest();
        Page<AlertResponse> result = detectionService.listAlerts(request, tenantId, false);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).ruleName()).isEqualTo("test-rule");
        assertThat(result.getContent().get(0).tenantId()).isEqualTo(tenantId);
    }

    @Test
    void superAdminSeesAllTenants() {
        DetectionAlert alert = createAlert();
        when(alertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alert)));

        AlertQueryRequest request = new AlertQueryRequest();
        Page<AlertResponse> result = detectionService.listAlerts(request, null, true);

        assertThat(result.getContent()).hasSize(1);
        verify(alertRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void acknowledgeAlertUpdatesFields() {
        UUID alertId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        DetectionAlert alert = createAlert();

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        detectionService.acknowledgeAlert(alertId, actorId);

        assertThat(alert.isAcknowledged()).isTrue();
        assertThat(alert.getAcknowledgedBy()).isEqualTo(actorId);
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void acknowledgeAlertThrowsWhenNotFound() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> detectionService.acknowledgeAlert(alertId, UUID.randomUUID()))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Alert not found");
    }

    @Test
    void countActiveAlertsDelegatesToRepository() {
        when(alertRepository.count(any(Specification.class))).thenReturn(3L);

        long count = detectionService.countActiveAlerts(tenantId);

        assertThat(count).isEqualTo(3);
    }

    private DetectionAlert createAlert() {
        DetectionAlert alert = new DetectionAlert();
        alert.setId(UUID.randomUUID());
        alert.setRuleName("test-rule");
        alert.setSeverity("HIGH");
        alert.setTitle("Test alert");
        alert.setTenantId(tenantId);
        alert.setCreatedAt(Instant.now());
        return alert;
    }
}
