package com.traceguard.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI traceguardOpenAPI() {
        var jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT obtido via POST /api/v1/auth/login");

        var apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API key gerada via POST /api/v1/admin/api-keys");

        var components = new Components()
                .addSecuritySchemes("bearer-jwt", jwtScheme)
                .addSecuritySchemes("api-key", apiKeyScheme);

        return new OpenAPI()
                .info(new Info()
                        .title("TraceGuard Audit Platform API")
                        .description("API de governança, auditoria e segurança com autenticação JWT, RBAC, rate limiting e multi-tenancy.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TraceGuard")
                                .email("support@traceguard.local"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .components(components)
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearer-jwt"));
    }
}
