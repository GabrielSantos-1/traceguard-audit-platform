package com.traceguard.integration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_configs")
public class IntegrationConfig {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getProvider() { return provider; }
    public String getName() { return name; }
    public String getConfigJson() { return configJson; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setName(String name) { this.name = name; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
