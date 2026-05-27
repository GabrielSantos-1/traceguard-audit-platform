package com.traceguard.monitoring.detection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DetectionAlertRepository extends JpaRepository<DetectionAlert, UUID>, JpaSpecificationExecutor<DetectionAlert> {
}
