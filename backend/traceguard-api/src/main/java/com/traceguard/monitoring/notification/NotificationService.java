package com.traceguard.monitoring.notification;

import com.traceguard.monitoring.detection.DetectionAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationConfigRepository configRepository;
    private final Map<String, NotificationChannel> channels;

    public NotificationService(
            NotificationConfigRepository configRepository,
            List<NotificationChannel> channelList
    ) {
        this.configRepository = configRepository;
        this.channels = channelList.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, c -> c));
    }

    @Async
    public void notifyAlerts(List<DetectionAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return;

        Map<UUID, List<DetectionAlert>> byTenant = alerts.stream()
                .filter(a -> a.getTenantId() != null)
                .collect(Collectors.groupingBy(DetectionAlert::getTenantId));

        for (Map.Entry<UUID, List<DetectionAlert>> entry : byTenant.entrySet()) {
            UUID tenantId = entry.getKey();
            List<DetectionAlert> tenantAlerts = entry.getValue();

            List<NotificationConfig> configs = configRepository.findByTenantIdAndEnabledTrue(tenantId);

            for (NotificationConfig config : configs) {
                NotificationChannel channel = channels.get(config.getChannel());
                if (channel == null) {
                    log.debug("No channel implementation for type: {}", config.getChannel());
                    continue;
                }

                String[] severities = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};
                int minIdx = java.util.Arrays.asList(severities).indexOf(config.getMinSeverity());
                if (minIdx < 0) minIdx = 0;

                for (DetectionAlert alert : tenantAlerts) {
                    int alertIdx = java.util.Arrays.asList(severities).indexOf(alert.getSeverity());
                    if (alertIdx < 0) alertIdx = 0;
                    if (alertIdx > minIdx) continue;

                    if (config.getRuleFilter() != null && !config.getRuleFilter().isBlank()
                            && !alert.getRuleName().matches(config.getRuleFilter())) {
                        continue;
                    }

                    try {
                        channel.send(alert, config);
                    } catch (Exception e) {
                        log.warn("Notification failed for alert {} via {}: {}", alert.getId(), config.getChannel(), e.getMessage());
                    }
                }
            }
        }
    }
}
