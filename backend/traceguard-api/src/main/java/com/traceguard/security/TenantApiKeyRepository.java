package com.traceguard.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, UUID> {
    Optional<TenantApiKey> findByKeyHash(String keyHash);
    List<TenantApiKey> findByEnabledTrueAndRevokedAtIsNull();
}
