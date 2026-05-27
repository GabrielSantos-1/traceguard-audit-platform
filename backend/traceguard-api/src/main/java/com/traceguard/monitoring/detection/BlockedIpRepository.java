package com.traceguard.monitoring.detection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, UUID> {

    List<BlockedIp> findByTenantIdAndActiveTrue(UUID tenantId);

    List<BlockedIp> findByIpAndActiveTrue(String ip);
}
