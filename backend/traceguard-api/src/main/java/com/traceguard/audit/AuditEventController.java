package com.traceguard.audit;

import com.traceguard.audit.dto.AuditEventResponse;
import com.traceguard.audit.dto.AuditEventQueryRequest;
import com.traceguard.audit.dto.CreateAuditEventRequest;
import com.traceguard.audit.dto.CreateAuditEventResponse;
import com.traceguard.common.api.ApiMeta;
import com.traceguard.common.api.ApiSuccessResponse;
import com.traceguard.common.web.CorrelationIdUtil;
import com.traceguard.common.util.IpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit/events")
public class AuditEventController {

    private final AuditEventService service;

    public AuditEventController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping
    public ApiSuccessResponse<List<AuditEventResponse>> list(
            @Valid AuditEventQueryRequest query,
            HttpServletRequest request
    ) {
        Instant s = (query.getStart() != null) ? query.getStart() : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant e = (query.getEnd() != null) ? query.getEnd() : Instant.now();
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;
        String sortBy = query.getSortBy() != null ? query.getSortBy() : "occurredAt";
        String sortDirection = query.getSortDirection() != null ? query.getSortDirection() : "desc";

        Page<AuditEventResponse> pageResult = service.list(
                query.getSeverity(),
                query.getUserId(),
                query.getAction(),
                s,
                e,
                page,
                size,
                sortBy,
                sortDirection
        );

        return ApiSuccessResponse.of(
                pageResult.getContent(),
                ApiMeta.paged(
                        CorrelationIdUtil.getOrCreate(request),
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.hasNext(),
                        pageResult.hasPrevious()
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<CreateAuditEventResponse>> create(
            @Valid @RequestBody CreateAuditEventRequest body,
            HttpServletRequest request
    ) {
        String ip = IpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");

        UUID id = service.create(body, ip, userAgent);

        return ResponseEntity
                .created(URI.create("/api/v1/audit/events/" + id))
                .body(ApiSuccessResponse.of(
                        new CreateAuditEventResponse(id.toString()),
                        ApiMeta.basic(CorrelationIdUtil.getOrCreate(request))
                ));
    }
}
