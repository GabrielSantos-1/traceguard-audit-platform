package com.traceguard.monitoring.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.traceguard.monitoring.detection.DetectionAlert;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sends alert notifications by e-mail.
 *
 * <p>Registered only when mail is actually configured ({@code spring.mail.host}). Without this
 * condition the bean would require a {@code JavaMailSender} that Spring Boot does not create when
 * no mail host is set, and the whole application would fail to start — which is exactly what
 * happened on every profile, including the documented "no Docker" quickstart.
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;

    public EmailNotificationChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public void send(DetectionAlert alert, NotificationConfig config) {
        if (config == null || config.getConfig() == null || config.getConfig().get("to") == null) {
            log.warn("Email recipient not configured for tenant {}", config != null ? config.getTenantId() : null);
            return;
        }

        String to = config.getConfig().get("to").asText();
        String from = config.getConfig().has("from") ? config.getConfig().get("from").asText() : "traceguard@localhost";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("[TraceGuard] " + alert.getSeverity() + ": " + alert.getTitle());

            String html = buildEmailBody(alert);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email notification for alert {}: {}", alert.getId(), e.getMessage());
        }
    }

    private String buildEmailBody(DetectionAlert alert) {
        String color = switch (alert.getSeverity()) {
            case "CRITICAL" -> "#ff0000";
            case "HIGH" -> "#ff6600";
            case "MEDIUM" -> "#ffcc00";
            default -> "#3366ff";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family: sans-serif; padding: 20px;\">");
        sb.append("<div style=\"border-left: 4px solid ").append(color).append("; padding: 16px; background: #f9f9f9;\">");
        sb.append("<h2 style=\"color: ").append(color).append("; margin: 0 0 12px 0;\">").append(alert.getTitle()).append("</h2>");
        sb.append("<table style=\"border-collapse: collapse; width: 100%;\">");
        sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold; width: 100px;\">Severity</td><td>").append(alert.getSeverity()).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold;\">Rule</td><td>").append(alert.getRuleName()).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold;\">Tenant</td><td>").append(alert.getTenantId()).append("</td></tr>");
        if (alert.getUserId() != null) {
            sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold;\">User</td><td>").append(alert.getUserId()).append("</td></tr>");
        }
        if (alert.getSourceIp() != null) {
            sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold;\">IP</td><td>").append(alert.getSourceIp()).append("</td></tr>");
        }
        sb.append("<tr><td style=\"padding: 4px 8px; font-weight: bold;\">Time</td><td>").append(alert.getCreatedAt()).append("</td></tr>");
        sb.append("</table>");
        sb.append("<p style=\"margin-top: 12px;\">").append(alert.getDescription() != null ? alert.getDescription() : "").append("</p>");
        sb.append("</div></body></html>");
        return sb.toString();
    }
}
