package com.traceguard.monitoring.detection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocked_ips")
public class BlockedIp {

    @Id
    private UUID id;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(length = 255)
    private String reason;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (blockedAt == null) blockedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getIp() { return ip; }
    public UUID getTenantId() { return tenantId; }
    public String getRuleName() { return ruleName; }
    public String getReason() { return reason; }
    public Instant getBlockedAt() { return blockedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }

    public void setId(UUID id) { this.id = id; }
    public void setIp(String ip) { this.ip = ip; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setReason(String reason) { this.reason = reason; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setActive(boolean active) { this.active = active; }
}
