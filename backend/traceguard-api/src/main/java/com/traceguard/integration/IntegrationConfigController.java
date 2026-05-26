package com.traceguard.integration;

import com.traceguard.auth.JwtPrincipal;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.integration.dto.IntegrationItem;
import com.traceguard.integration.dto.SaveIntegrationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
public class IntegrationConfigController {

    private final IntegrationConfigService service;

    public IntegrationConfigController(IntegrationConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiSuccessResponse<List<IntegrationItem>> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request) {
        var items = service.list(principal.tenantId()).stream()
                .map(c -> new IntegrationItem(c.getProvider(), c.getName(), c.getConfigJson(), c.isEnabled()))
                .toList();
        return ApiSuccessResponse.of(items, ApiMeta.basic(CorrelationIdUtil.getOrCreate(request)));
    }

    @GetMapping("/{provider}")
    public ApiSuccessResponse<IntegrationItem> get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String provider,
            HttpServletRequest request) {
        var c = service.get(principal.tenantId(), provider);
        var item = new IntegrationItem(c.getProvider(), c.getName(), c.getConfigJson(), c.isEnabled());
        return ApiSuccessResponse.of(item, ApiMeta.basic(CorrelationIdUtil.getOrCreate(request)));
    }

    @PutMapping("/{provider}")
    public ApiSuccessResponse<IntegrationItem> save(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String provider,
            @RequestBody SaveIntegrationRequest body,
            HttpServletRequest request) {
        var c = service.save(principal.tenantId(), provider, body.name(), body.configJson(), body.enabled());
        var item = new IntegrationItem(c.getProvider(), c.getName(), c.getConfigJson(), c.isEnabled());
        return ApiSuccessResponse.of(item, ApiMeta.basic(CorrelationIdUtil.getOrCreate(request)));
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String provider) {
        service.delete(principal.tenantId(), provider);
        return ResponseEntity.noContent().build();
    }
}
