package com.SIGMA.USCO.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI sigmaOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("SIGMA API")
                        .description("API para la gestión integral de modalidades de grado en la Facultad de Ingeniería de la Universidad Surcolombiana.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Equipo SIGMA").email("soporte@sigma.usco.edu.co"))
                        .license(new License().name("MIT License")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación Técnica SIGMA")
                        .url("https://github.com/USCO/SIGMA"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Autenticación JWT Bearer Token")
                        )
                );
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .pathsToExclude("/error")
                .build();
    }
}



