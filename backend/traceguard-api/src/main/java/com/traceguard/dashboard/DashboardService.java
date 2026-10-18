package com.traceguard.dashboard;

import com.traceguard.audit.AuditEvent;
import com.traceguard.audit.AuditEventRepository;
import com.traceguard.auth.UserAccountRepository;
import com.traceguard.auth.UserStatus;
import com.traceguard.dashboard.dto.*;
import com.traceguard.tenant.TenantRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AuditEventRepository auditEventRepository;
    private final UserAccountRepository userAccountRepository;
    private final TenantRepository tenantRepository;

    public DashboardService(AuditEventRepository auditEventRepository,
                            UserAccountRepository userAccountRepository,
                            TenantRepository tenantRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userAccountRepository = userAccountRepository;
        this.tenantRepository = tenantRepository;
    }

    public DashboardKpiResponse getKpi(UUID tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(java.time.Duration.ofHours(24));
        Instant dayBefore = now.minus(java.time.Duration.ofHours(48));

        long events24h = auditEventRepository.countByTenantIdAndOccurredAtBetween(tenantId, yesterday, now);
        long eventsPrevious = auditEventRepository.countByTenantIdAndOccurredAtBetween(tenantId, dayBefore, yesterday);

        long alerts24h = auditEventRepository.countByTenantIdAndSeverityInAndOccurredAtBetween(
                tenantId, List.of("HIGH", "CRITICAL"), yesterday, now);
        long alertsPrevious = auditEventRepository.countByTenantIdAndSeverityInAndOccurredAtBetween(
                tenantId, List.of("HIGH", "CRITICAL"), dayBefore, yesterday);

        long activeUsers = userAccountRepository.countByStatus(UserStatus.ACTIVE);
        long totalTenants = tenantRepository.count();

        Double eventsChange = changePercent(events24h, eventsPrevious);
        Double alertsChange = changePercent(alerts24h, alertsPrevious);

        // There is no history of user status, so no baseline exists to compare against.
        // null means "cannot be computed" and the dashboard renders "—" instead of inventing 0%.
        Double usersChange = null;

        return new DashboardKpiResponse(events24h, alerts24h, activeUsers, totalTenants,
                eventsChange, alertsChange, usersChange);
    }

    /**
     * Percentage change of the current window against the previous one.
     *
     * <p>Returns {@code null} when the previous window was empty and the current one is not: there is
     * no baseline to compare against, and the previous implementation answered {@code 100.0} in that
     * case, so the dashboard reported "100% growth" on a period that had zero events — in both windows.
     * Zero over zero is a real "no change" and returns {@code 0}.
     */
    private static Double changePercent(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : null;
        }
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }

    public List<SeverityDistributionItem> getSeverityDistribution(UUID tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(java.time.Duration.ofHours(24));

        List<Object[]> raw = auditEventRepository.countBySeverityGrouped(tenantId, yesterday, now);
        long total = raw.stream().mapToLong(r -> (Long) r[1]).sum();

        return raw.stream()
                .map(r -> {
                    String severity = (String) r[0];
                    long count = (Long) r[1];
                    double pct = total == 0 ? 0 : Math.round(((double) count / total) * 1000.0) / 10.0;
                    return new SeverityDistributionItem(severity, count, pct);
                })
                .collect(Collectors.toList());
    }

    public List<TimelineItem> getTimeline(UUID tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(java.time.Duration.ofHours(24));

        List<AuditEvent> events = auditEventRepository
                .findByTenantIdAndOccurredAtBetweenOrderByOccurredAtAsc(tenantId, yesterday, now);

        Map<Integer, Long> hourMap = new HashMap<>();
        for (AuditEvent e : events) {
            int h = e.getOccurredAt().atZone(ZoneId.of("America/Sao_Paulo")).getHour();
            hourMap.merge(h, 1L, Long::sum);
        }

        List<TimelineItem> result = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            int h = yesterday.atZone(ZoneId.of("America/Sao_Paulo")).plusHours(i).getHour();
            result.add(new TimelineItem(String.format("%02d:00", h), hourMap.getOrDefault(h, 0L)));
        }
        return result;
    }

    public List<RecentEventItem> getRecentEvents(UUID tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(java.time.Duration.ofHours(24));

        return auditEventRepository
                .findByTenantIdAndOccurredAtBetween(tenantId, yesterday, now,
                        org.springframework.data.domain.PageRequest.of(0, 7,
                                org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"))
                .getContent().stream()
                .map(e -> new RecentEventItem(e.getId(), e.getAction(), buildDescription(e),
                        e.getSeverity(), e.getIp(), e.getOccurredAt()))
                .collect(Collectors.toList());
    }

    public List<CriticalAlertItem> getCriticalAlerts(UUID tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(java.time.Duration.ofHours(24));

        return auditEventRepository
                .findByTenantIdAndSeverityInAndOccurredAtBetween(tenantId,
                        List.of("HIGH", "CRITICAL"), yesterday, now,
                        org.springframework.data.domain.PageRequest.of(0, 5,
                                org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"))
                .getContent().stream()
                .map(e -> {
                    String title = switch (e.getAction()) {
                        case "LOGIN_FAILED" -> "Múltiplas falhas de login";
                        case "ACCESS_DENIED" -> "Acesso não autorizado";
                        case "API_KEY_CREATED" -> "Nova chave de API criada";
                        default -> "Evento: " + e.getAction();
                    };
                    String desc = e.getIp() != null
                            ? "IP " + e.getIp() + " — " + e.getAction()
                            : e.getAction() + " — " + (e.getEntity() != null ? e.getEntity() : "N/A");
                    return new CriticalAlertItem(e.getId(), title, desc, e.getSeverity(), e.getIp(), e.getOccurredAt());
                })
                .collect(Collectors.toList());
    }

    private String buildDescription(AuditEvent e) {
        if (e.getEntity() != null && e.getEntityId() != null) {
            return e.getEntity() + " " + e.getEntityId().substring(0, Math.min(8, e.getEntityId().length()));
        }
        if (e.getIp() != null) return "IP " + e.getIp();
        return e.getAction();
    }
}
