package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ApiKeyAbuseRule implements DetectionRule {

    private static final String RULE_NAME = "api-key-abuse-detection";
    private static final int MIN_HISTORY_FOR_ALERT = 5;

    private final ObjectMapper objectMapper;
    private final Map<UUID, List<String>> knownIpsByTenant = new ConcurrentHashMap<>();

    public ApiKeyAbuseRule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public Severity getSeverity() {
        return Severity.HIGH;
    }

    @Override
    public List<DetectionAlert> evaluate(List<UserActivityEvent> activities) {
        List<UserActivityEvent> apiActivities = activities.stream()
                .filter(a -> a.getHttpPath() != null)
                .filter(a -> a.getHttpPath().startsWith("/api/v1/audit")
                        || a.getHttpPath().startsWith("/api/v1/dashboard"))
                .filter(a -> a.getIp() != null)
                .filter(a -> a.getTenantId() != null)
                .toList();

        Map<UUID, List<UserActivityEvent>> byTenant = apiActivities.stream()
                .collect(Collectors.groupingBy(UserActivityEvent::getTenantId));

        List<DetectionAlert> alerts = new ArrayList<>();

        for (Map.Entry<UUID, List<UserActivityEvent>> entry : byTenant.entrySet()) {
            UUID tenantId = entry.getKey();
            List<UserActivityEvent> tenantActivities = entry.getValue();

            List<String> knownIps = knownIpsByTenant
                    .computeIfAbsent(tenantId, k -> new ArrayList<>());

            for (UserActivityEvent activity : tenantActivities) {
                String ip = activity.getIp();

                if (!knownIps.contains(ip)) {
                    knownIps.add(ip);

                    if (knownIps.size() > MIN_HISTORY_FOR_ALERT && knownIps.size() <= MIN_HISTORY_FOR_ALERT + 1) {
                        ObjectNode evidence = objectMapper.createObjectNode();
                        evidence.put("tenant_id", tenantId.toString());
                        evidence.put("new_ip", ip);
                        evidence.put("known_ip_count", knownIps.size() - 1);

                        var knownIpsArray = objectMapper.createArrayNode();
                        knownIps.stream().filter(i -> !i.equals(ip)).limit(10).forEach(knownIpsArray::add);
                        evidence.set("known_ips_sample", knownIpsArray);

                        DetectionAlert alert = new DetectionAlert();
                        alert.setRuleName(RULE_NAME);
                        alert.setSeverity(Severity.HIGH.name());
                        alert.setTitle("API key used from new IP: " + ip);
                        alert.setDescription(String.format(
                                "Tenant %s API key activity detected from new IP %s (%d known IPs in history)",
                                tenantId, ip, knownIps.size() - 1));
                        alert.setTenantId(tenantId);
                        alert.setSourceIp(ip);
                        alert.setEvidence(evidence);

                        alerts.add(alert);
                    }
                }
            }

            if (knownIps.size() > 100) {
                knownIps.subList(0, knownIps.size() - 50).clear();
            }
        }

        return alerts;
    }
}
