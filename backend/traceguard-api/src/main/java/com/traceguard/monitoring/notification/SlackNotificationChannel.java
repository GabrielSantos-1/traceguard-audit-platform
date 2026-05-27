package com.traceguard.monitoring.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.detection.DetectionAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SlackNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationChannel.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SlackNotificationChannel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String getType() {
        return "SLACK";
    }

    @Override
    public void send(DetectionAlert alert, NotificationConfig config) {
        if (config == null || config.getConfig() == null || config.getConfig().get("webhook_url") == null) {
            log.warn("Slack webhook URL not configured for tenant {}", config != null ? config.getTenantId() : null);
            return;
        }

        String webhookUrl = config.getConfig().get("webhook_url").asText();

        String severityEmoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> ":rotating_light:";
            case "HIGH" -> ":warning:";
            case "MEDIUM" -> ":large_orange_diamond:";
            case "LOW" -> ":information_source:";
            default -> ":bell:";
        };

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("text", severityEmoji + " *TraceGuard Alert*: " + alert.getTitle());

        ObjectNode attachment = objectMapper.createObjectNode();
        attachment.put("color", switch (alert.getSeverity()) {
            case "CRITICAL" -> "#ff0000";
            case "HIGH" -> "#ff6600";
            case "MEDIUM" -> "#ffcc00";
            default -> "#3366ff";
        });

        ObjectNode fields = objectMapper.createObjectNode();
        fields.put("title", alert.getTitle());
        fields.put("value",
                "Severity: " + alert.getSeverity()
                        + "\nRule: " + alert.getRuleName()
                        + "\nTenant: " + alert.getTenantId()
                        + (alert.getUserId() != null ? "\nUser: " + alert.getUserId() : "")
                        + (alert.getSourceIp() != null ? "\nIP: " + alert.getSourceIp() : "")
                        + "\n" + (alert.getDescription() != null ? alert.getDescription() : ""));
        fields.put("short", false);
        attachment.set("fields", objectMapper.createArrayNode().add(fields));

        payload.set("attachments", objectMapper.createArrayNode().add(attachment));

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Slack webhook returned {} for alert {}: {}", response.statusCode(), alert.getId(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to send Slack notification for alert {}: {}", alert.getId(), e.getMessage());
        }
    }
}
