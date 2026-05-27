package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GeoAnomalyRule implements DetectionRule {

    private static final String RULE_NAME = "geo-anomaly-detection";
    private static final int MIN_IPS_FOR_ALERT = 3;

    private final ObjectMapper objectMapper;
    private final Map<UUID, List<String>> knownIpsByUser = new ConcurrentHashMap<>();

    public GeoAnomalyRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        List<DetectionAlert> alerts = new ArrayList<>();

        for (UserActivityEvent activity : activities) {
            if (activity.getUserId() == null || activity.getIp() == null) continue;
            if (activity.getHttpPath() != null
                    && (activity.getHttpPath().startsWith("/api/v1/audit")
                    || activity.getHttpPath().startsWith("/api/v1/dashboard"))) {
                continue;
            }

            UUID userId = activity.getUserId();
            String ip = activity.getIp();

            List<String> knownIps = knownIpsByUser.computeIfAbsent(userId, k -> new ArrayList<>());

            if (!knownIps.contains(ip)) {
                knownIps.add(ip);

                if (knownIps.size() >= MIN_IPS_FOR_ALERT) {
                    ObjectNode evidence = objectMapper.createObjectNode();
                    evidence.put("user_id", userId.toString());
                    evidence.put("new_ip", ip);
                    evidence.put("known_ip_count", knownIps.size() - 1);

                    var knownIpsArray = objectMapper.createArrayNode();
                    knownIps.stream().filter(i -> !i.equals(ip)).limit(10).forEach(knownIpsArray::add);
                    evidence.set("known_ips", knownIpsArray);

                    DetectionAlert alert = new DetectionAlert();
                    alert.setRuleName(RULE_NAME);
                    alert.setSeverity(Severity.MEDIUM.name());
                    alert.setTitle("User login from new IP: " + ip);
                    alert.setDescription(String.format(
                            "User %s accessed from new IP %s (%d distinct IPs in history)",
                            userId, ip, knownIps.size()));
                    alert.setTenantId(activity.getTenantId());
                    alert.setUserId(userId);
                    alert.setSourceIp(ip);
                    alert.setEvidence(evidence);

                    alerts.add(alert);
                }
            }

            if (knownIps.size() > 100) {
                knownIps.subList(0, knownIps.size() - 50).clear();
            }
        }

        return alerts;
    }
}
