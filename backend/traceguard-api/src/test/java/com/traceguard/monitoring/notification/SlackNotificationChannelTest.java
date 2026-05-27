package com.traceguard.monitoring.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.detection.DetectionAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SlackNotificationChannelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SlackNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new SlackNotificationChannel(objectMapper);
    }

    @Test
    void getTypeReturnsSlack() {
        assertThat(channel.getType()).isEqualTo("SLACK");
    }

    @Test
    void handlesMissingWebhookUrlGracefully() {
        DetectionAlert alert = createAlert("HIGH", "brute-force-detection", UUID.randomUUID());
        NotificationConfig config = createConfig(null);

        assertThatCode(() -> channel.send(alert, config))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesEmptyConfigGracefully() {
        DetectionAlert alert = createAlert("HIGH", "brute-force-detection", UUID.randomUUID());
        NotificationConfig config = new NotificationConfig();
        config.setChannel("SLACK");
        config.setEnabled(true);

        assertThatCode(() -> channel.send(alert, config))
                .doesNotThrowAnyException();
    }

    @Test
    void handlesSendWithNullConfigGracefully() {
        DetectionAlert alert = createAlert("HIGH", "brute-force-detection", UUID.randomUUID());

        assertThatCode(() -> channel.send(alert, null))
                .doesNotThrowAnyException();
    }

    private DetectionAlert createAlert(String severity, String ruleName, UUID tenantId) {
        DetectionAlert alert = new DetectionAlert();
        alert.setId(UUID.randomUUID());
        alert.setRuleName(ruleName);
        alert.setSeverity(severity);
        alert.setTitle("Test alert: " + ruleName);
        alert.setDescription("This is a test alert");
        alert.setTenantId(tenantId);
        alert.setSourceIp("192.168.1.1");
        alert.setCreatedAt(Instant.now());
        return alert;
    }

    private NotificationConfig createConfig(String webhookUrl) {
        NotificationConfig config = new NotificationConfig();
        config.setId(UUID.randomUUID());
        config.setChannel("SLACK");
        config.setEnabled(true);
        config.setMinSeverity("LOW");

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (webhookUrl != null) {
            node.put("webhook_url", webhookUrl);
        }
        config.setConfig(node);

        return config;
    }
}
