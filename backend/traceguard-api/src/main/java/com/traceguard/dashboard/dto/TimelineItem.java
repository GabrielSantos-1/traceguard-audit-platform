package com.traceguard.dashboard.dto;

import java.time.Instant;

public record TimelineItem(
        String hour,
        long count
) {}
