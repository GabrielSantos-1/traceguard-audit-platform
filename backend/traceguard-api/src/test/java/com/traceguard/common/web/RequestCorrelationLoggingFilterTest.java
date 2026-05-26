package com.traceguard.common.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestCorrelationLoggingFilterTest {

    @Test
    void propagatesIncomingCorrelationId() throws ServletException, IOException {
        RequestCorrelationLoggingFilter filter = new RequestCorrelationLoggingFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        request.addHeader(CorrelationIdUtil.HEADER_NAME, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> response.setStatus(204);
        filter.doFilter(request, response, chain);

        assertEquals("corr-123", response.getHeader(CorrelationIdUtil.HEADER_NAME));
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        RequestCorrelationLoggingFilter filter = new RequestCorrelationLoggingFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> response.setStatus(200);
        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(CorrelationIdUtil.HEADER_NAME));
    }
}
