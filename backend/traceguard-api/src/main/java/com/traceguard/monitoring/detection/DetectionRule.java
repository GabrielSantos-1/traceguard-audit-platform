package com.traceguard.monitoring.detection;

import com.traceguard.monitoring.activity.UserActivityEvent;

import java.util.List;

public interface DetectionRule {

    String getName();

    Severity getSeverity();

    List<DetectionAlert> evaluate(List<UserActivityEvent> activities);
}
