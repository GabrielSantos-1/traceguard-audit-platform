package com.traceguard.security;

import jakarta.persistence.*;
import java.util.HashSet;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenant_api_keys")
public class TenantApiKey {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Set<ApiKeyScope> scopes = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<ApiKeyScope> getScopes() { return scopes; }
    public boolean isEnabled() { return enabled; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setName(String name) { this.name = name; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public void setScopes(Set<ApiKeyScope> scopes) {
        this.scopes = (scopes == null) ? new HashSet<>() : new HashSet<>(scopes);
    }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public boolean isRevoked() { return revokedAt != null; }
}
