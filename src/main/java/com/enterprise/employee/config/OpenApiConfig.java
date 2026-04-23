package com.enterprise.employee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 global configuration.
 *
 * <p>Registers:
 * <ul>
 *   <li>API metadata (title, version, description, contact, licence).</li>
 *   <li>A global {@code BasicAuth} security scheme that mirrors the Spring Security
 *       configuration, so the Swagger UI "Authorize" button works out of the box.</li>
 * </ul>
 *
 * <p>The Swagger UI is served at {@code /swagger-ui.html} and the raw OpenAPI
 * spec at {@code /api-docs} as configured in {@code application.yml}.
 */
@Configuration
public class OpenApiConfig {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String SECURITY_SCHEME_NAME = "basicAuth";

    // ── Bean ──────────────────────────────────────────────────────────────────

    /**
     * Produces the top-level {@link OpenAPI} descriptor consumed by SpringDoc
     * to generate the API specification.
     *
     * @return a fully configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI employeeOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, basicAuthScheme()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the {@link Info} block displayed in the Swagger UI header.
     */
    private Info apiInfo() {
        return new Info()
                .title("Employee Management API")
                .description(
                        "Enterprise REST API for managing employee records. "
                        + "Supports single and batch operations with full CRUD "
                        + "and partial name search capabilities.")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Enterprise Backend Team")
                        .email("backend-team@enterprise.com"))
                .license(new License()
                        .name("Internal Use Only")
                        .url("https://enterprise.com/licenses/internal"));
    }

    /**
     * Declares the HTTP Basic Authentication security scheme so the Swagger UI
     * renders an "Authorize" dialog where testers can supply credentials.
     */
    private SecurityScheme basicAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("HTTP Basic Authentication — provide username and password.");
    }
}
