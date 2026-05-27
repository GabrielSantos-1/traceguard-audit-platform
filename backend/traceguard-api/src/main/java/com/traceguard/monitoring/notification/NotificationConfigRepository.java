package com.traceguard.monitoring.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, UUID> {

    List<NotificationConfig> findByTenantIdAndEnabledTrue(UUID tenantId);

    List<NotificationConfig> findByTenantIdAndEnabledTrueAndMinSeverityIn(UUID tenantId, List<String> severities);
}
