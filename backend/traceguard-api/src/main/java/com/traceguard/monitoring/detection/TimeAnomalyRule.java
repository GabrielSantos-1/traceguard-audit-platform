package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TimeAnomalyRule implements DetectionRule {

    private static final String RULE_NAME = "time-anomaly-detection";
    private static final int MIN_SAMPLES = 15;

    private final ObjectMapper objectMapper;
    private final Map<UUID, UserTimeProfile> profiles = new java.util.concurrent.ConcurrentHashMap<>();

    public TimeAnomalyRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public Severity getSeverity() {
        return Severity.LOW;
    }

    @Override
    public List<DetectionAlert> evaluate(List<UserActivityEvent> activities) {
        List<DetectionAlert> alerts = new ArrayList<>();

        for (UserActivityEvent activity : activities) {
            if (activity.getUserId() == null || activity.getOccurredAt() == null) continue;
            if (activity.getTenantId() == null) continue;

            UUID userId = activity.getUserId();
            int hour = activity.getOccurredAt().atZone(java.time.ZoneOffset.UTC).getHour();

            UserTimeProfile profile = profiles.computeIfAbsent(userId, k -> new UserTimeProfile());
            profile.recordActivity(hour);

            if (profile.totalActivities() >= MIN_SAMPLES
                    && !profile.isWithinTypicalRange(hour)) {

                ObjectNode evidence = objectMapper.createObjectNode();
                evidence.put("user_id", userId.toString());
                evidence.put("activity_hour", hour);
                evidence.put("typical_range_start", profile.typicalStart);
                evidence.put("typical_range_end", profile.typicalEnd);
                evidence.put("total_activities", profile.totalActivities());

                DetectionAlert alert = new DetectionAlert();
                alert.setRuleName(RULE_NAME);
                alert.setSeverity(Severity.LOW.name());
                alert.setTitle("Unusual activity hour for user " + userId);
                alert.setDescription(String.format(
                        "User %s active at hour %d (typical range: %d-%d, %d total activities)",
                        userId, hour, profile.typicalStart, profile.typicalEnd, profile.totalActivities()));
                alert.setTenantId(activity.getTenantId());
                alert.setUserId(userId);
                alert.setSourceIp(activity.getIp());
                alert.setEvidence(evidence);

                alerts.add(alert);
            }
        }

        return alerts;
    }

    private static class UserTimeProfile {
        private final int[] hourCounts = new int[24];
        private int total = 0;
        private int typicalStart = 8;
        private int typicalEnd = 18;

        synchronized void recordActivity(int hour) {
            if (hour >= 0 && hour < 24) {
                hourCounts[hour]++;
                total++;
            }
            recalcRange();
        }

        synchronized boolean isWithinTypicalRange(int hour) {
            if (typicalStart <= typicalEnd) {
                return hour >= typicalStart && hour <= typicalEnd;
            }
            return hour >= typicalStart || hour <= typicalEnd;
        }

        synchronized int totalActivities() {
            return total;
        }

        private void recalcRange() {
            if (total < MIN_SAMPLES) return;
            int peak = 0;
            for (int i = 0; i < 24; i++) {
                if (hourCounts[i] > hourCounts[peak]) peak = i;
            }
            int cumulative = 0;
            int start = peak;
            while (start > 0 && cumulative < total * 0.4) {
                start--;
                cumulative += hourCounts[start];
            }
            cumulative = 0;
            int end = peak;
            while (end < 23 && cumulative < total * 0.4) {
                end++;
                cumulative += hourCounts[end];
            }
            typicalStart = Math.max(0, start);
            typicalEnd = Math.min(23, end);
        }
    }
}
