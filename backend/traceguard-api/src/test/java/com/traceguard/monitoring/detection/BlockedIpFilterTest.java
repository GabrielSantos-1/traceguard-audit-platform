package com.traceguard.monitoring.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockedIpFilterTest {

    @Mock
    private BlockedIpRepository blockedIpRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BlockedIpFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BlockedIpFilter(blockedIpRepository, objectMapper);
    }

    @Test
    void allowsRequestWhenIpNotBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/detection/alerts");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        when(blockedIpRepository.findByIpAndActiveTrue("10.0.0.1")).thenReturn(List.of());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void blocksRequestWhenIpIsBlocked() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getAttribute(com.traceguard.common.web.CorrelationIdUtil.ATTRIBUTE_NAME)).thenReturn("corr-123");

        BlockedIp blocked = new BlockedIp();
        blocked.setId(UUID.randomUUID());
        blocked.setIp("192.168.1.1");
        blocked.setActive(true);
        when(blockedIpRepository.findByIpAndActiveTrue("192.168.1.1")).thenReturn(List.of(blocked));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        String json = stringWriter.toString();
        assertThat(json).contains("IP_BLOCKED");
        assertThat(json).contains("corr-123");
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/index.html");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(blockedIpRepository, never()).findByIpAndActiveTrue(anyString());
    }

    @Test
    void skipsActuatorPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsSwaggerPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void handlesNullIpGracefully() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(blockedIpRepository, never()).findByIpAndActiveTrue(anyString());
    }

    @Test
    void blocksMultipleIpsIndependently() throws Exception {
        // First IP is blocked
        when(request.getRequestURI()).thenReturn("/api/v1/admin/detection/alerts");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getAttribute(com.traceguard.common.web.CorrelationIdUtil.ATTRIBUTE_NAME)).thenReturn("corr-1");

        BlockedIp blocked = new BlockedIp();
        blocked.setId(UUID.randomUUID());
        blocked.setIp("10.0.0.1");
        blocked.setActive(true);
        when(blockedIpRepository.findByIpAndActiveTrue("10.0.0.1")).thenReturn(List.of(blocked));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
