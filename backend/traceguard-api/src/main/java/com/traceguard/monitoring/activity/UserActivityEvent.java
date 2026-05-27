package com.traceguard.monitoring.activity;

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
@Table(name = "user_activity_log")
public class UserActivityEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 200)
    private String resourceId;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_path", nullable = false, length = 500)
    private String httpPath;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(nullable = false)
    private boolean success;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private JsonNode beforeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    private JsonNode afterSnapshot;

    @Column(name = "diff_summary", columnDefinition = "TEXT")
    private String diffSummary;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getHttpMethod() { return httpMethod; }
    public String getHttpPath() { return httpPath; }
    public int getHttpStatus() { return httpStatus; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public String getCorrelationId() { return correlationId; }
    public boolean isSuccess() { return success; }
    public JsonNode getBeforeSnapshot() { return beforeSnapshot; }
    public JsonNode getAfterSnapshot() { return afterSnapshot; }
    public String getDiffSummary() { return diffSummary; }
    public Long getDurationMs() { return durationMs; }
    public Instant getOccurredAt() { return occurredAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public void setAction(String action) { this.action = action; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setHttpPath(String httpPath) { this.httpPath = httpPath; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public void setIp(String ip) { this.ip = ip; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setBeforeSnapshot(JsonNode beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public void setAfterSnapshot(JsonNode afterSnapshot) { this.afterSnapshot = afterSnapshot; }
    public void setDiffSummary(String diffSummary) { this.diffSummary = diffSummary; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
