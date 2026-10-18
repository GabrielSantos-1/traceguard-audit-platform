package com.traceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.auth.JwtAuthFilter;
import com.traceguard.auth.JwtTokenService;
import com.traceguard.common.web.RequestCorrelationLoggingFilter;
import com.traceguard.monitoring.activity.UserActivityFilter;
import com.traceguard.monitoring.activity.UserActivityService;
import com.traceguard.monitoring.detection.BlockedIpFilter;
import com.traceguard.monitoring.detection.BlockedIpRepository;
import com.traceguard.ratelimit.RateLimitFilter;
import com.traceguard.ratelimit.RateLimiterService;
import com.traceguard.tenant.TenantRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
public class SecurityConfig {

        @Bean
        SecurityFilterChain apiFilterChain(
                        HttpSecurity http,
                        ApiKeyService apiKeyService,
                        JwtTokenService jwtTokenService,
                        TenantRepository tenantRepository,
                        RateLimiterService rateLimiter,
                        MeterRegistry meterRegistry,
                        ObjectMapper objectMapper,
                        UserActivityService userActivityService,
                        BlockedIpRepository blockedIpRepository,
                        @Value("${traceguard.rate-limit.login-per-minute}") int loginPerMinute,
                        @Value("${traceguard.rate-limit.refresh-per-minute}") int refreshPerMinute,
                        @Value("${traceguard.rate-limit.audit-ingest-per-minute}") int auditIngestPerMinute,
                        @Value("${traceguard.rate-limit.audit-query-per-minute}") int auditQueryPerMinute,
                        @Value("${traceguard.rate-limit.admin-read-per-minute}") int adminReadPerMinute,
                        @Value("${traceguard.rate-limit.admin-write-per-minute}") int adminWritePerMinute)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers
                                        .frameOptions(fo -> fo.disable())
                                        .contentTypeOptions(cto -> cto.disable())
                                        .httpStrictTransportSecurity(hsts -> hsts
                                                .includeSubDomains(true)
                                                .maxAgeInSeconds(31536000))
                                        .xssProtection(xss -> xss.disable()))
                                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .logout(logout -> logout.disable())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                                (request, response, authException) -> SecurityErrorResponseWriter.write(
                                                                objectMapper,
                                                                request,
                                                                response,
                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                "UNAUTHORIZED",
                                                                "Authentication is required"))
                                                .accessDeniedHandler(
                                                                (request, response,
                                                                                accessDeniedException) -> SecurityErrorResponseWriter
                                                                                                .write(
                                                                                                                objectMapper,
                                                                                                                request,
                                                                                                                response,
                                                                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                                                                "FORBIDDEN",
                                                                                                                "Operation is not allowed")))
                                .addFilterBefore(
                                                new SecurityHeadersFilter(),
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(
                                                new RequestCorrelationLoggingFilter(meterRegistry),
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(
                                                new BlockedIpFilter(blockedIpRepository, objectMapper),
                                                AuthorizationFilter.class)
                                .addFilterBefore(
                                                new UserActivityFilter(userActivityService, objectMapper),
                                                AuthorizationFilter.class)
                                .addFilterBefore(
                                                new JwtAuthFilter(jwtTokenService, objectMapper, tenantRepository, meterRegistry),
                                                AuthorizationFilter.class)
                                .addFilterBefore(
                                                new ApiKeyFilter(apiKeyService, objectMapper),
                                                AuthorizationFilter.class)
                                .addFilterBefore(
                                                new RateLimitFilter(
                                                                rateLimiter,
                                                                objectMapper,
                                                                meterRegistry,
                                                                loginPerMinute,
                                                                refreshPerMinute,
                                                                auditIngestPerMinute,
                                                                auditQueryPerMinute,
                                                                adminReadPerMinute,
                                                                adminWritePerMinute),
                                                AuthorizationFilter.class)
                                .authorizeHttpRequests(auth -> auth
                                                // Dashboard static resources. The React build emits hashed files under
                                                // /assets plus its favicon; the old single-file dashboard had everything
                                                // inline and needed none of this. Without these the browser receives 401
                                                // for the bundle and the page renders blank.
                                                .requestMatchers(
                                                                "/",
                                                                "/index.html",
                                                                "/favicon.ico",
                                                                "/favicon.svg",
                                                                "/icons.svg",
                                                                "/manifest.webmanifest",
                                                                "/assets/**",
                                                                "/*.js",
                                                                "/*.css",
                                                                "/*.png",
                                                                "/*.svg",
                                                                "/*.ico",
                                                                "/*.webp",
                                                                "/*.map")
                                                .permitAll()
                                                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/audit/events").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/tenants")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/tenants/**")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/tenants")
                                                .hasAnyRole("SUPER_ADMIN", "AUDITOR")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit-events")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN")
                                                .requestMatchers("/api/v1/admin/activity/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR")
                                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/api-keys")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "OPERATOR")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/api-keys")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR", "OPERATOR")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/api-keys/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "OPERATOR")
                                                .requestMatchers("/api/v1/admin/dashboard/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR", "OPERATOR")
                                .requestMatchers("/api/v1/admin/detection/alerts/**")
                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR")
                                .requestMatchers("/api/v1/admin/detection/blocked-ips/**")
                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/integrations/**")
                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/integrations/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/integrations/**")
                                                .hasAnyRole("SUPER_ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/account/**")
                                                .authenticated()
                                                .anyRequest().authenticated());

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
