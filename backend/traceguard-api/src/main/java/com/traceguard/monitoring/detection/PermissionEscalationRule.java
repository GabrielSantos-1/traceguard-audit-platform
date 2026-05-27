package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PermissionEscalationRule implements DetectionRule {

    private static final String RULE_NAME = "permission-escalation-detection";

    private final ObjectMapper objectMapper;
    private final int threshold;
    private final int windowMinutes;

    public PermissionEscalationRule(
            ObjectMapper objectMapper,
            @Value("${traceguard.detection.permission-escalation.threshold:5}") int threshold,
            @Value("${traceguard.detection.permission-escalation.window-minutes:5}") int windowMinutes
    ) {
        this.objectMapper = objectMapper;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public Severity getSeverity() {
        return Severity.MEDIUM;
    }

    @Override
    public List<DetectionAlert> evaluate(List<UserActivityEvent> activities) {
        Instant windowStart = Instant.now().minusSeconds(windowMinutes * 60L);

        Map<UUID, List<UserActivityEvent>> byUser = activities.stream()
                .filter(a -> a.getUserId() != null)
                .filter(a -> a.getHttpStatus() == 403)
                .filter(a -> a.getOccurredAt() != null && a.getOccurredAt().isAfter(windowStart))
                .collect(Collectors.groupingBy(UserActivityEvent::getUserId));

        List<DetectionAlert> alerts = new ArrayList<>();

        for (Map.Entry<UUID, List<UserActivityEvent>> entry : byUser.entrySet()) {
            UUID userId = entry.getKey();
            List<UserActivityEvent> deniedAttempts = entry.getValue();

            List<String> distinctEndpoints = deniedAttempts.stream()
                    .map(UserActivityEvent::getHttpPath)
                    .distinct()
                    .toList();

            if (distinctEndpoints.size() < threshold) continue;

            UUID tenantId = deniedAttempts.stream()
                    .map(UserActivityEvent::getTenantId)
                    .filter(t -> t != null)
                    .findFirst().orElse(null);

            if (tenantId == null) continue;

            String userRole = deniedAttempts.stream()
                    .map(UserActivityEvent::getUserRole)
                    .filter(r -> r != null)
                    .findFirst().orElse("UNKNOWN");

            ArrayNode endpoints = objectMapper.createArrayNode();
            distinctEndpoints.forEach(endpoints::add);

            ObjectNode evidence = objectMapper.createObjectNode();
            evidence.put("user_id", userId.toString());
            evidence.put("user_role", userRole);
            evidence.put("forbidden_count", deniedAttempts.size());
            evidence.put("distinct_endpoints", distinctEndpoints.size());
            evidence.put("threshold", threshold);
            evidence.put("window_minutes", windowMinutes);
            evidence.set("endpoints", endpoints);

            DetectionAlert alert = new DetectionAlert();
            alert.setRuleName(RULE_NAME);
            alert.setSeverity(Severity.MEDIUM.name());
            alert.setTitle("Possible permission escalation by user " + userId);
            alert.setDescription(String.format(
                    "User %s (role: %s) received %d forbidden responses across %d distinct endpoints in %d minutes",
                    userId, userRole, deniedAttempts.size(), distinctEndpoints.size(), windowMinutes));
            alert.setTenantId(tenantId);
            alert.setUserId(userId);
            alert.setEvidence(evidence);

            alerts.add(alert);
        }

        return alerts;
    }
}
