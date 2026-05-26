package com.traceguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceguard.auth.JwtAuthFilter;
import com.traceguard.auth.JwtTokenService;
import com.traceguard.common.web.RequestCorrelationLoggingFilter;
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
                        @Value("${traceguard.rate-limit.login-per-minute}") int loginPerMinute,
                        @Value("${traceguard.rate-limit.refresh-per-minute}") int refreshPerMinute,
                        @Value("${traceguard.rate-limit.audit-ingest-per-minute}") int auditIngestPerMinute,
                        @Value("${traceguard.rate-limit.audit-query-per-minute}") int auditQueryPerMinute,
                        @Value("${traceguard.rate-limit.admin-read-per-minute}") int adminReadPerMinute,
                        @Value("${traceguard.rate-limit.admin-write-per-minute}") int adminWritePerMinute)
                        throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers.frameOptions(fo -> fo.disable()))
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
                                                new RequestCorrelationLoggingFilter(meterRegistry),
                                                UsernamePasswordAuthenticationFilter.class)
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
                                                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
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
                                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/api-keys")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "OPERATOR")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/api-keys")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR", "OPERATOR")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/api-keys/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "OPERATOR")
                                                .requestMatchers("/api/v1/admin/dashboard/**")
                                                .hasAnyRole("SUPER_ADMIN", "TENANT_ADMIN", "AUDITOR", "OPERATOR")
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
