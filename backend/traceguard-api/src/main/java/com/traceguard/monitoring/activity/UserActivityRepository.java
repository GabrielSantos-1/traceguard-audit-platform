package com.traceguard.monitoring.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivityEvent, UUID>, JpaSpecificationExecutor<UserActivityEvent> {
}
