package com.traceguard.monitoring.detection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public class AlertQueryRequest {

    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Invalid UUID format")
    private String userId;

    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Invalid UUID format")
    private String tenantId;

    private String ruleName;

    private String severity;

    private Boolean acknowledged;

    private Instant from;

    private Instant to;

    @Min(0)
    private int page;

    @Min(1)
    @Max(100)
    private int size;

    private String sort;

    public AlertQueryRequest() {
        this.page = 0;
        this.size = 20;
        this.sort = "-createdAt";
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }

    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }

    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
