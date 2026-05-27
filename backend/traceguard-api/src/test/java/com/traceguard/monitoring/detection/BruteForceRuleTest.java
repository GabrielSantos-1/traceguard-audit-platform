package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.monitoring.activity.UserActivityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BruteForceRuleTest {

    @Mock
    private BlockedIpRepository blockedIpRepository;

    @Captor
    private ArgumentCaptor<BlockedIp> blockedIpCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID tenantId = UUID.randomUUID();
    private BruteForceRule rule;

    @BeforeEach
    void setUp() {
        rule = new BruteForceRule(null, blockedIpRepository, objectMapper, 3, 5);
    }

    @Test
    void createsAlertWhenThresholdExceeded() {
        List<UserActivityEvent> activities = IntStream.range(0, 5)
                .mapToObj(i -> createEvent("192.168.1.1", false, "/api/v1/auth/login"))
                .toList();

        when(blockedIpRepository.findByIpAndActiveTrue("192.168.1.1")).thenReturn(List.of());
        when(blockedIpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DetectionAlert> alerts = rule.evaluate(activities);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRuleName()).isEqualTo("brute-force-detection");
        assertThat(alerts.get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(alerts.get(0).getSourceIp()).isEqualTo("192.168.1.1");
        assertThat(alerts.get(0).getTenantId()).isEqualTo(tenantId);

        verify(blockedIpRepository).save(blockedIpCaptor.capture());
        assertThat(blockedIpCaptor.getValue().getIp()).isEqualTo("192.168.1.1");
        assertThat(blockedIpCaptor.getValue().isActive()).isTrue();
    }

    @Test
    void noAlertBelowThreshold() {
        List<UserActivityEvent> activities = IntStream.range(0, 2)
                .mapToObj(i -> createEvent("192.168.1.2", false, "/api/v1/auth/login"))
                .toList();

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    @Test
    void noAlertWhenAlreadyBlocked() {
        List<UserActivityEvent> activities = IntStream.range(0, 5)
                .mapToObj(i -> createEvent("10.0.0.1", false, "/api/v1/auth/login"))
                .toList();

        BlockedIp existing = new BlockedIp();
        existing.setIp("10.0.0.1");
        existing.setTenantId(tenantId);
        existing.setActive(true);
        when(blockedIpRepository.findByIpAndActiveTrue("10.0.0.1")).thenReturn(List.of(existing));

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    @Test
    void ignoresSuccessfulLogins() {
        List<UserActivityEvent> activities = IntStream.range(0, 5)
                .mapToObj(i -> createEvent("192.168.1.3", true, "/api/v1/auth/login"))
                .toList();

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).isEmpty();
    }

    @Test
    void handlesMultipleIpsIndependently() {
        List<UserActivityEvent> activities = new ArrayList<>();
        activities.addAll(IntStream.range(0, 5)
                .mapToObj(i -> createEvent("10.0.0.1", false, "/api/v1/auth/login"))
                .toList());
        activities.addAll(IntStream.range(0, 5)
                .mapToObj(i -> createEvent("10.0.0.2", false, "/api/v1/auth/login"))
                .toList());

        when(blockedIpRepository.findByIpAndActiveTrue("10.0.0.1")).thenReturn(List.of());
        when(blockedIpRepository.findByIpAndActiveTrue("10.0.0.2")).thenReturn(List.of());
        when(blockedIpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DetectionAlert> alerts = rule.evaluate(activities);
        assertThat(alerts).hasSize(2);
    }

    private UserActivityEvent createEvent(String ip, boolean success, String path) {
        UserActivityEvent event = new UserActivityEvent();
        event.setIp(ip);
        event.setSuccess(success);
        event.setHttpPath(path);
        event.setTenantId(tenantId);
        event.setOccurredAt(Instant.now());
        return event;
    }
}
