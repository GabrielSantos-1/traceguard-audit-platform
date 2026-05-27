package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PermissionEscalationRuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private PermissionEscalationRule rule;

    @BeforeEach
    void setUp() {
        rule = new PermissionEscalationRule(objectMapper, 3, 5);
    }

    @Test
    void createsAlertWhenThresholdExceeded() {
        List<UserActivityEvent> activities = List.of(
                createForbidden(userId, "/api/v1/admin/tenants"),
                createForbidden(userId, "/api/v1/admin/api-keys"),
                createForbidden(userId, "/api/v1/admin/integrations"),
                createForbidden(userId, "/api/v1/admin/detection/alerts")
        );

        List<DetectionAlert> alerts = rule.evaluate(activities);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRuleName()).isEqualTo("permission-escalation-detection");
        assertThat(alerts.get(0).getSeverity()).isEqualTo("MEDIUM");
        assertThat(alerts.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void noAlertBelowThreshold() {
        List<UserActivityEvent> activities = List.of(
                createForbidden(userId, "/api/v1/admin/tenants"),
                createForbidden(userId, "/api/v1/admin/api-keys")
        );

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    @Test
    void filtersNonForbiddenStatuses() {
        UserActivityEvent ok = new UserActivityEvent();
        ok.setUserId(userId);
        ok.setHttpStatus(200);
        ok.setTenantId(tenantId);
        ok.setOccurredAt(Instant.now());

        List<UserActivityEvent> activities = List.of(ok, ok, ok, ok);
        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    @Test
    void countsDistinctEndpointsNotTotalAttempts() {
        UserActivityEvent forbidden = createForbidden(userId, "/api/v1/admin/tenants");
        List<UserActivityEvent> activities = IntStream.range(0, 10)
                .mapToObj(i -> forbidden)
                .toList();

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    private UserActivityEvent createForbidden(UUID uid, String path) {
        UserActivityEvent event = new UserActivityEvent();
        event.setUserId(uid);
        event.setHttpStatus(403);
        event.setHttpPath(path);
        event.setTenantId(tenantId);
        event.setOccurredAt(Instant.now());
        event.setUserRole("OPERATOR");
        return event;
    }
}
