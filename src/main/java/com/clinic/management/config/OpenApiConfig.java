package com.clinic.management.config;

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

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI clinicManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic Management & Patient Queue API")
                        .description("Production-grade RESTful API for small clinics to manage patient records, " +
                                "schedule appointments with collision validation, and automate real-time patient queues.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Clinic Engineering Team")
                                .email("engineering@clinic.com")
                                .url("https://github.com/clinic/clinic-management"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token obtained from `/api/v1/auth/login`")));
    }
}
