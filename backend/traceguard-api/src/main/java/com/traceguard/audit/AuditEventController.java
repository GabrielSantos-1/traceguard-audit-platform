package com.traceguard.audit;

import com.traceguard.audit.dto.AuditEventResponse;
import com.traceguard.audit.dto.CreateAuditEventRequest;
import com.traceguard.common.util.IpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditEventController {

    private final AuditEventService service;

    public AuditEventController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditEventResponse> list(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant s = (start != null) ? start : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant e = (end != null) ? end : Instant.now();

        return service.list(severity, userId, action, s, e, page, size);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody CreateAuditEventRequest body,
            HttpServletRequest request
    ) {
        String ip = IpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");

        UUID id = service.create(body, ip, userAgent);

        return ResponseEntity
                .created(URI.create("/api/v1/audit/events/" + id))
                .body(Map.of("id", id.toString()));
    }
}
