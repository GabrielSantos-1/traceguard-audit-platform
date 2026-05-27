package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traceguard.monitoring.activity.UserActivityEvent;
import com.traceguard.monitoring.activity.UserActivityRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BruteForceRule implements DetectionRule {

    private static final Logger log = LoggerFactory.getLogger(BruteForceRule.class);
    private static final String RULE_NAME = "brute-force-detection";

    private final UserActivityRepository activityRepository;
    private final BlockedIpRepository blockedIpRepository;
    private final ObjectMapper objectMapper;

    private final int threshold;
    private final int windowMinutes;

    public BruteForceRule(
            UserActivityRepository activityRepository,
            BlockedIpRepository blockedIpRepository,
            ObjectMapper objectMapper,
            @Value("${traceguard.detection.brute-force.threshold:5}") int threshold,
            @Value("${traceguard.detection.brute-force.window-minutes:5}") int windowMinutes
    ) {
        this.activityRepository = activityRepository;
        this.blockedIpRepository = blockedIpRepository;
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
        return Severity.HIGH;
    }

    @Override
    public List<DetectionAlert> evaluate(List<UserActivityEvent> activities) {
        Instant windowStart = Instant.now().minusSeconds(windowMinutes * 60L);

        Map<String, List<UserActivityEvent>> byIp = activities.stream()
                .filter(a -> !a.isSuccess())
                .filter(a -> a.getIp() != null)
                .filter(a -> a.getHttpPath() != null && a.getHttpPath().contains("/auth/login"))
                .filter(a -> a.getOccurredAt() != null && a.getOccurredAt().isAfter(windowStart))
                .collect(Collectors.groupingBy(UserActivityEvent::getIp));

        List<DetectionAlert> alerts = new ArrayList<>();

        for (Map.Entry<String, List<UserActivityEvent>> entry : byIp.entrySet()) {
            String ip = entry.getKey();
            List<UserActivityEvent> attempts = entry.getValue();

            if (attempts.size() < threshold) continue;

            UUID tenantId = attempts.stream()
                    .map(UserActivityEvent::getTenantId)
                    .filter(t -> t != null)
                    .findFirst().orElse(null);

            if (tenantId == null) continue;

            var activeBlocks = blockedIpRepository.findByIpAndActiveTrue(ip);
            boolean alreadyBlocked = activeBlocks.stream()
                    .anyMatch(b -> tenantId.equals(b.getTenantId()));
            if (alreadyBlocked) continue;

            ArrayNode timestamps = objectMapper.createArrayNode();
            attempts.stream()
                    .map(a -> a.getOccurredAt().toString())
                    .forEach(timestamps::add);

            ObjectNode evidence = objectMapper.createObjectNode();
            evidence.put("ip", ip);
            evidence.put("attempts", attempts.size());
            evidence.put("threshold", threshold);
            evidence.put("window_minutes", windowMinutes);
            evidence.set("timestamps", timestamps);
            evidence.put("unique_endpoints", attempts.stream()
                    .map(UserActivityEvent::getHttpPath)
                    .distinct().count());

            DetectionAlert alert = new DetectionAlert();
            alert.setRuleName(RULE_NAME);
            alert.setSeverity(Severity.HIGH.name());
            alert.setTitle("Possible brute force attack from " + ip);
            alert.setDescription(String.format(
                    "%d failed login attempts from %s in %d minutes (threshold: %d)",
                    attempts.size(), ip, windowMinutes, threshold));
            alert.setTenantId(tenantId);
            alert.setSourceIp(ip);
            alert.setEvidence(evidence);

            alerts.add(alert);

            BlockedIp blockedIp = new BlockedIp();
            blockedIp.setIp(ip);
            blockedIp.setTenantId(tenantId);
            blockedIp.setRuleName(RULE_NAME);
            blockedIp.setReason("Brute force: " + attempts.size() + " failed logins in " + windowMinutes + "m");
            blockedIp.setExpiresAt(Instant.now().plusSeconds(1800));
            blockedIp.setActive(true);

            try {
                blockedIpRepository.save(blockedIp);
            } catch (Exception e) {
                log.warn("Failed to save blocked IP {}: {}", ip, e.getMessage());
            }
        }

        return alerts;
    }
}
