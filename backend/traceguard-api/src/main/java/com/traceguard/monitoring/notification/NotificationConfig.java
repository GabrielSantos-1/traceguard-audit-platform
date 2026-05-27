package com.traceguard.monitoring.notification;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_configs")
public class NotificationConfig {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false)
    private boolean enabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode config;

    @Column(name = "min_severity", nullable = false, length = 20)
    private String minSeverity;

    @Column(name = "rule_filter", length = 255)
    private String ruleFilter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getChannel() { return channel; }
    public boolean isEnabled() { return enabled; }
    public JsonNode getConfig() { return config; }
    public String getMinSeverity() { return minSeverity; }
    public String getRuleFilter() { return ruleFilter; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setConfig(JsonNode config) { this.config = config; }
    public void setMinSeverity(String minSeverity) { this.minSeverity = minSeverity; }
    public void setRuleFilter(String ruleFilter) { this.ruleFilter = ruleFilter; }
}
