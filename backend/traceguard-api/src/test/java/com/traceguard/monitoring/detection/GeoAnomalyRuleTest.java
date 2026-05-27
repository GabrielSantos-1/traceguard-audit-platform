package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeoAnomalyRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private GeoAnomalyRule rule;

    @BeforeEach
    void setUp() {
        rule = new GeoAnomalyRule(objectMapper);
    }

    @Test
    void noAlertForFirstFewIps() {
        UserActivityEvent e1 = createEvent(userId, "10.0.0.1");
        UserActivityEvent e2 = createEvent(userId, "10.0.0.2");

        List<DetectionAlert> alerts1 = rule.evaluate(List.of(e1));
        assertThat(alerts1).isEmpty();

        List<DetectionAlert> alerts2 = rule.evaluate(List.of(e2));
        assertThat(alerts2).isEmpty();
    }

    @Test
    void alertsOnThirdNewIp() {
        rule.evaluate(List.of(createEvent(userId, "10.0.0.1")));
        rule.evaluate(List.of(createEvent(userId, "10.0.0.2")));

        UserActivityEvent newIp = createEvent(userId, "10.0.0.3");
        List<DetectionAlert> alerts = rule.evaluate(List.of(newIp));

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRuleName()).isEqualTo("geo-anomaly-detection");
        assertThat(alerts.get(0).getSeverity()).isEqualTo("MEDIUM");
        assertThat(alerts.get(0).getSourceIp()).isEqualTo("10.0.0.3");
    }

    @Test
    void noAlertForRepeatedIp() {
        UserActivityEvent event = createEvent(userId, "10.0.0.1");
        rule.evaluate(List.of(event));

        List<DetectionAlert> alerts = rule.evaluate(List.of(event));
        assertThat(alerts).isEmpty();
    }

    @Test
    void ignoresAuditPaths() {
        UserActivityEvent audit = createEvent(userId, "10.0.0.1");
        audit.setHttpPath("/api/v1/audit/events");
        rule.evaluate(List.of(audit));

        UserActivityEvent newIp = createEvent(userId, "10.0.0.2");
        newIp.setHttpPath("/api/v1/audit/events");
        List<DetectionAlert> alerts = rule.evaluate(List.of(newIp));

        assertThat(alerts).isEmpty();
    }

    private UserActivityEvent createEvent(UUID uid, String ip) {
        UserActivityEvent event = new UserActivityEvent();
        event.setUserId(uid);
        event.setIp(ip);
        event.setTenantId(tenantId);
        event.setOccurredAt(Instant.now());
        event.setHttpPath("/api/v1/admin/activity");
        return event;
    }
}
