package com.traceguard.audit;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String entity;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String severity;

    @Column(length = 100)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private JsonNode beforeData;

      @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private JsonNode afterData;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }

    // getters/setters (simples, sem Lombok)
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getAction() { return action; }
    public String getEntity() { return entity; }
    public String getEntityId() { return entityId; }
    public String getSeverity() { return severity; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public Instant getOccurredAt() { return occurredAt; }
    public JsonNode getMetadata() { return metadata; }
    public JsonNode getBeforeData() { return beforeData; }
    public JsonNode getAfterData() { return afterData; }

    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setAction(String action) { this.action = action; }
    public void setEntity(String entity) { this.entity = entity; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setIp(String ip) { this.ip = ip; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setMetadata(JsonNode metadata) { this.metadata = metadata; }
    public void setBeforeData(JsonNode beforeData) { this.beforeData = beforeData; }
            public void setAfterData(JsonNode afterData) { this.afterData = afterData; }
}