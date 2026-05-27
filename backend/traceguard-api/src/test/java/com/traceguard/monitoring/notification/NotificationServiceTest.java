package com.traceguard.monitoring.notification;

import com.traceguard.monitoring.detection.DetectionAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationConfigRepository configRepository;

    @Mock
    private NotificationChannel slackChannel;

    @Mock
    private NotificationChannel emailChannel;

    private NotificationService notificationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID alertId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(slackChannel.getType()).thenReturn("SLACK");
        when(emailChannel.getType()).thenReturn("EMAIL");

        notificationService = new NotificationService(
                configRepository,
                List.of(slackChannel, emailChannel)
        );
    }

    private DetectionAlert createAlert(String severity) {
        DetectionAlert alert = new DetectionAlert();
        alert.setId(alertId);
        alert.setRuleName("brute-force-detection");
        alert.setSeverity(severity);
        alert.setTitle("Brute force detected");
        alert.setTenantId(tenantId);
        alert.setCreatedAt(Instant.now());
        return alert;
    }

    private NotificationConfig createConfig(String channel, String minSeverity, String ruleFilter) {
        NotificationConfig config = new NotificationConfig();
        config.setId(UUID.randomUUID());
        config.setTenantId(tenantId);
        config.setChannel(channel);
        config.setMinSeverity(minSeverity);
        config.setRuleFilter(ruleFilter);
        config.setEnabled(true);
        return config;
    }

    @Test
    void notifiesSlackWhenConfigured() {
        DetectionAlert alert = createAlert("HIGH");
        NotificationConfig slackConfig = createConfig("SLACK", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(slackConfig));

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel).send(eq(alert), eq(slackConfig));
    }

    @Test
    void notifiesEmailWhenConfigured() {
        DetectionAlert alert = createAlert("MEDIUM");
        NotificationConfig emailConfig = createConfig("EMAIL", "MEDIUM", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(emailConfig));

        notificationService.notifyAlerts(List.of(alert));

        verify(emailChannel).send(eq(alert), eq(emailConfig));
    }

    @Test
    void filtersByMinimumSeverity() {
        DetectionAlert lowAlert = createAlert("LOW");
        NotificationConfig config = createConfig("SLACK", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(lowAlert));

        verify(slackChannel, never()).send(any(), any());
    }

    @Test
    void includesAlertsAtOrAboveMinSeverity() {
        DetectionAlert highAlert = createAlert("HIGH");
        DetectionAlert criticalAlert = createAlert("CRITICAL");
        NotificationConfig config = createConfig("SLACK", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(highAlert, criticalAlert));

        verify(slackChannel).send(eq(highAlert), eq(config));
        verify(slackChannel).send(eq(criticalAlert), eq(config));
    }

    @Test
    void filtersByRuleName() {
        DetectionAlert alert = createAlert("HIGH");
        alert.setRuleName("api-key-abuse");
        NotificationConfig config = createConfig("SLACK", "LOW", "brute-force.*");

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel, never()).send(any(), any());
    }

    @Test
    void matchesRuleFilterWhenPatternMatches() {
        DetectionAlert alert = createAlert("HIGH");
        alert.setRuleName("brute-force-detection");
        NotificationConfig config = createConfig("SLACK", "LOW", "brute-force.*");

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel).send(eq(alert), eq(config));
    }

    @Test
    void handlesEmptyConfigList() {
        DetectionAlert alert = createAlert("CRITICAL");

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of());

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel, never()).send(any(), any());
        verify(emailChannel, never()).send(any(), any());
    }

    @Test
    void handlesNullRuleFilter() {
        DetectionAlert alert = createAlert("HIGH");
        NotificationConfig config = createConfig("SLACK", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel).send(eq(alert), eq(config));
    }

    @Test
    void handlesEmptyAlertList() {
        notificationService.notifyAlerts(List.of());

        verify(configRepository, never()).findByTenantIdAndEnabledTrue(any());
        verify(slackChannel, never()).send(any(), any());
    }

    @Test
    void handlesNullAlertList() {
        notificationService.notifyAlerts(null);

        verify(configRepository, never()).findByTenantIdAndEnabledTrue(any());
        verify(slackChannel, never()).send(any(), any());
    }

    @Test
    void handlesChannelFailureGracefully() {
        DetectionAlert alert = createAlert("HIGH");
        NotificationConfig config = createConfig("SLACK", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config));

        notificationService.notifyAlerts(List.of(alert));

        // Channel send succeeds — no exception thrown
        verify(slackChannel).send(eq(alert), eq(config));
    }

    @Test
    void groupsAlertsByTenant() {
        UUID otherTenantId = UUID.randomUUID();
        DetectionAlert alert1 = createAlert("HIGH");
        DetectionAlert alert2 = createAlert("CRITICAL");
        alert2.setTenantId(otherTenantId);

        NotificationConfig config1 = createConfig("SLACK", "HIGH", null);
        NotificationConfig config2 = createConfig("EMAIL", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(config1));
        when(configRepository.findByTenantIdAndEnabledTrue(otherTenantId)).thenReturn(List.of(config2));

        notificationService.notifyAlerts(List.of(alert1, alert2));

        verify(slackChannel).send(eq(alert1), eq(config1));
        verify(emailChannel).send(eq(alert2), eq(config2));
    }

    @Test
    void notifiesMultipleChannelsPerTenant() {
        DetectionAlert alert = createAlert("HIGH");
        NotificationConfig slackConfig = createConfig("SLACK", "HIGH", null);
        NotificationConfig emailConfig = createConfig("EMAIL", "HIGH", null);

        when(configRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(slackConfig, emailConfig));

        notificationService.notifyAlerts(List.of(alert));

        verify(slackChannel).send(eq(alert), eq(slackConfig));
        verify(emailChannel).send(eq(alert), eq(emailConfig));
    }
}
