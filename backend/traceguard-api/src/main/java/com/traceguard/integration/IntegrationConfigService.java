package com.traceguard.integration;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class IntegrationConfigService {

    private final IntegrationConfigRepository repository;

    public IntegrationConfigService(IntegrationConfigRepository repository) {
        this.repository = repository;
    }

    public List<IntegrationConfig> list(UUID tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public IntegrationConfig get(UUID tenantId, String provider) {
        return repository.findByTenantIdAndProvider(tenantId, provider)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Integration not found"));
    }

    public IntegrationConfig save(UUID tenantId, String provider, String name, String configJson, boolean enabled) {
        var existing = repository.findByTenantIdAndProvider(tenantId, provider);
        IntegrationConfig cfg;
        if (existing.isPresent()) {
            cfg = existing.get();
            cfg.setName(name);
            cfg.setConfigJson(configJson);
            cfg.setEnabled(enabled);
        } else {
            cfg = new IntegrationConfig();
            cfg.setTenantId(tenantId);
            cfg.setProvider(provider);
            cfg.setName(name);
            cfg.setConfigJson(configJson);
            cfg.setEnabled(enabled);
        }
        return repository.save(cfg);
    }

    public void delete(UUID tenantId, String provider) {
        var cfg = get(tenantId, provider);
        repository.delete(cfg);
    }
}
