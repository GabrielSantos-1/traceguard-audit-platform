package com.traceguard.monitoring.notification;

import com.traceguard.monitoring.detection.DetectionAlert;

public interface NotificationChannel {

    String getType();

    void send(DetectionAlert alert, NotificationConfig config);
}
