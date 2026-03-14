package com.gamifyapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger / SpringDoc OpenAPI 3.
 * Disponível em: /swagger-ui.html e /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GamifyAPI")
                .description("Gamificação como Serviço (GaaS) — API REST multi-tenant. " +
                             "Integre mecânicas de XP, níveis, conquistas, streaks e rankings " +
                             "em qualquer aplicação via API Key.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("GamifyAPI Team")
                    .email("suporte@gamifyapi.com")))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .addSecurityItem(new SecurityRequirement().addList("ApiKey"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtido via POST /api/v1/auth/login"))
                .addSecuritySchemes("ApiKey", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API Key obtida no painel do tenant admin")));
    }
}
