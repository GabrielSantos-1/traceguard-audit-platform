package com.traceguard.monitoring.activity.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ActivityQueryRequest {

    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "userId must be a valid UUID")
    private String userId;

    @Size(max = 100)
    private String action;

    @Size(max = 100)
    private String resourceType;

    private Boolean success;

    private Instant from;

    private Instant to;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(100)
    private Integer size = 20;

    @Pattern(
            regexp = "^-?(occurredAt|action|resourceType|userId|httpMethod|httpStatus|durationMs)$",
            message = "sort must be one of: occurredAt, action, resourceType, userId, httpMethod, httpStatus, durationMs (prefix with - for desc)")
    private String sort = "-occurredAt";

    @AssertTrue(message = "to must be greater than or equal to from")
    public boolean isRangeValid() {
        return from == null || to == null || !to.isBefore(from);
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
