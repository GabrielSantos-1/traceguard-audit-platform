package com.traceguard.tenant;

import com.traceguard.adminaudit.AdminAuditService;
import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.sanitization.InputSanitizer;
import com.traceguard.tenant.dto.CreateTenantRequest;
import com.traceguard.tenant.dto.TenantResponse;
import com.traceguard.tenant.dto.UpdateTenantRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository repository;
    private final AdminAuditService adminAuditService;
    private final InputSanitizer sanitizer;

    public TenantService(TenantRepository repository, AdminAuditService adminAuditService, InputSanitizer sanitizer) {
        this.repository = repository;
        this.adminAuditService = adminAuditService;
        this.sanitizer = sanitizer;
    }

    public TenantResponse create(CreateTenantRequest request, JwtPrincipal actor, String correlationId) {
        String slug = sanitizer.sanitizeText(request.slug(), 80, "slug");
        if (repository.findBySlug(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "slug already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(sanitizer.sanitizeText(request.name(), 150, "name"));
        tenant.setSlug(slug);
        tenant.setStatus(request.status() != null ? request.status() : TenantStatus.ACTIVE);
        Tenant saved = repository.save(tenant);

        adminAuditService.record(
                actor.userId(),
                saved.getId(),
                "TENANT_CREATED",
                "TENANT",
                saved.getId().toString(),
                correlationId,
                Map.of("slug", saved.getSlug(), "status", saved.getStatus().name())
        );

        return toResponse(saved);
    }

    public Page<TenantResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return repository.findAll(PageRequest.of(safePage, safeSize)).map(this::toResponse);
    }

    public TenantResponse update(UUID id, UpdateTenantRequest request, JwtPrincipal actor, String correlationId) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(sanitizer.sanitizeText(request.name(), 150, "name"));
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = sanitizer.sanitizeText(request.slug(), 80, "slug");
            if (repository.existsBySlugAndIdNot(slug, tenant.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "slug already exists");
            }
            tenant.setSlug(slug);
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }

        Tenant saved = repository.save(tenant);

        adminAuditService.record(
                actor.userId(),
                saved.getId(),
                "TENANT_UPDATED",
                "TENANT",
                saved.getId().toString(),
                correlationId,
                Map.of("slug", saved.getSlug(), "status", saved.getStatus().name())
        );
        return toResponse(saved);
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
