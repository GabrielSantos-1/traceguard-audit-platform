package com.traceguard.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenRecord, UUID> {

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    List<RefreshTokenRecord> findAllByFamilyId(UUID familyId);
}