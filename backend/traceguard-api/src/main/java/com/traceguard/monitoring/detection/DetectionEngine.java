package com.traceguard.monitoring.detection;

import com.traceguard.monitoring.activity.UserActivityEvent;
import com.traceguard.monitoring.activity.UserActivityRepository;
import com.traceguard.monitoring.notification.NotificationService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    private final UserActivityRepository activityRepository;
    private final DetectionAlertRepository alertRepository;
    private final NotificationService notificationService;
    private final List<DetectionRule> rules;

    private volatile Instant lastRunAt;

    public DetectionEngine(
            UserActivityRepository activityRepository,
            DetectionAlertRepository alertRepository,
            NotificationService notificationService,
            List<DetectionRule> rules
    ) {
        this.activityRepository = activityRepository;
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.rules = rules;
        this.lastRunAt = Instant.now();
    }

    @Scheduled(fixedDelayString = "${traceguard.detection.interval-ms:60000}")
    public void run() {
        Instant now = Instant.now();
        Instant since = lastRunAt;

        if (since == null) {
            lastRunAt = now;
            return;
        }

        try {
            List<UserActivityEvent> activities = activityRepository.findAll(
                    (root, query, cb) -> {
                        Predicate afterLastRun = cb.greaterThan(root.get("occurredAt"), since);
                        Predicate beforeOrAtNow = cb.lessThanOrEqualTo(root.get("occurredAt"), now);
                        return cb.and(afterLastRun, beforeOrAtNow);
                    },
                    Sort.by(Sort.Direction.ASC, "occurredAt")
            );

            if (activities.isEmpty()) {
                lastRunAt = now;
                return;
            }

            log.debug("Detection engine processing {} activities ({} rules)", activities.size(), rules.size());

            List<DetectionAlert> allAlerts = new ArrayList<>();

            for (DetectionRule rule : rules) {
                try {
                    List<DetectionAlert> alerts = rule.evaluate(activities);
                    if (!alerts.isEmpty()) {
                        allAlerts.addAll(alerts);
                        log.info("Rule '{}' generated {} alert(s)", rule.getName(), alerts.size());
                    }
                } catch (Exception e) {
                    log.warn("Rule '{}' failed: {}", rule.getName(), e.getMessage(), e);
                }
            }

            if (!allAlerts.isEmpty()) {
                alertRepository.saveAll(allAlerts);
                notificationService.notifyAlerts(allAlerts);
            }

            lastRunAt = now;

            if (rules.isEmpty()) {
                log.warn("No detection rules configured");
            }
        } catch (Exception e) {
            log.warn("Detection engine run failed: {}", e.getMessage(), e);
        }
    }
}
