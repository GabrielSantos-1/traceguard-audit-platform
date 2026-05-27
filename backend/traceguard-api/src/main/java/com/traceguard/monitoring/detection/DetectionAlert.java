package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "detection_alerts")
public class DetectionAlert {

    @Id
    private UUID id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode evidence;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRuleName() { return ruleName; }
    public String getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getSourceIp() { return sourceIp; }
    public JsonNode getEvidence() { return evidence; }
    public boolean isAcknowledged() { return acknowledged; }
    public UUID getAcknowledgedBy() { return acknowledgedBy; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public void setEvidence(JsonNode evidence) { this.evidence = evidence; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public void setAcknowledgedBy(UUID acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
