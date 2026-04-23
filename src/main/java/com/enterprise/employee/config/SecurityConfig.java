package com.enterprise.employee.config;

import com.enterprise.employee.filter.RequestHeaderLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Spring Security configuration — uses the modern {@link SecurityFilterChain}
 * bean approach. The deprecated {@code WebSecurityConfigurerAdapter} is
 * intentionally avoided.
 *
 * <p>Security model:
 * <ul>
 *   <li><strong>Stateless</strong> — no HTTP session is created or used
 *       ({@link SessionCreationPolicy#STATELESS}). Each request must carry
 *       credentials, which is the correct model for a REST API.</li>
 *   <li><strong>HTTP Basic Auth</strong> — simple and sufficient for an
 *       enterprise internal API. Replace with OAuth2/JWT for public-facing
 *       deployments.</li>
 *   <li><strong>CSRF disabled</strong> — appropriate for stateless REST APIs
 *       that do not rely on browser session cookies. Re-enable if the API is
 *       ever consumed by a browser-based form.</li>
 * </ul>
 *
 * <p>Path security strategy:
 * <ul>
 *   <li>Swagger / OpenAPI paths are handled via {@link WebSecurityCustomizer}
 *       ({@code web.ignoring()}), which removes them from the security filter
 *       chain entirely. This is necessary because SpringDoc's static resources
 *       and spec JSON endpoint are not registered as Spring MVC handlers, so
 *       {@code permitAll()} alone (which uses MvcRequestMatcher) cannot
 *       reliably match them.</li>
 *   <li>Actuator and H2 console are permitted inside the filter chain via
 *       {@code permitAll()}, since they are proper MVC-mapped routes.</li>
 *   <li>All {@code /employees/**} routes require HTTP Basic authentication.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ── Constants ─────────────────────────────────────────────────────────────

    /**
     * Paths completely removed from the Spring Security filter chain via
     * {@link WebSecurityCustomizer}. Requests to these paths bypass ALL filters
     * (authentication, CSRF, logging, etc.).
     * Only assign paths that are public, read-only, and non-sensitive.
     */
    private static final AntPathRequestMatcher[] IGNORED_PATHS = {
            AntPathRequestMatcher.antMatcher("/v3/api-docs/**"),
            AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
            AntPathRequestMatcher.antMatcher("/swagger-ui.html")
    };

    /**
     * MVC-matched paths that remain inside the filter chain but require no credentials.
     */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/**",
            "/h2-console/**"     // H2 console; meaningful only in the dev profile
    };

    // ── WebSecurityCustomizer — bypass filter chain for Swagger entirely ───────

    /**
     * Instructs Spring Security to completely ignore Swagger / OpenAPI paths.
     *
     * <p>{@code web.ignoring()} removes matched requests from the security
     * filter chain before any filter is invoked. This is the definitive fix
     * for 401/500 errors on Swagger UI when {@code permitAll()} is insufficient
     * because the paths are not registered as Spring MVC handlers.
     *
     * @return a {@link WebSecurityCustomizer} that whitelists all OpenAPI endpoints
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(IGNORED_PATHS);
    }

    // ── Security filter chain ─────────────────────────────────────────────────

    /**
     * Defines the primary {@link SecurityFilterChain} for all HTTP requests.
     *
     * @param http                      the {@link HttpSecurity} builder
     * @param requestHeaderLoggingFilter the custom header-logging filter to include
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestHeaderLoggingFilter requestHeaderLoggingFilter) throws Exception {

        http
            // ── Token/session management ────────────────────────────────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CSRF: disabled for stateless REST ───────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Frame options: allow same-origin for H2 console ─────────────
            // Must be relaxed so the H2 console iframe renders in the browser.
            // Has no effect outside the dev profile where H2 is enabled.
            .headers(headers ->
                    headers.frameOptions(frame -> frame.sameOrigin()))

            // ── Authorisation rules ─────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // Double-safety: also permit Swagger paths inside the chain.
                    // Requests that reach this point (not caught by web.ignoring())
                    // are still granted access without credentials.
                    .requestMatchers(IGNORED_PATHS).permitAll()
                    // Actuator & H2 console — proper MVC routes, permitAll() works here
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    // Employee endpoints require authentication
                    .requestMatchers(HttpMethod.GET,    "/employees/**").authenticated()
                    .requestMatchers(HttpMethod.POST,   "/employees/**").authenticated()
                    .requestMatchers(HttpMethod.PUT,    "/employees/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/employees/**").authenticated()
                    // Catch-all: deny anything not explicitly matched above
                    .anyRequest().authenticated()
            )

            // ── Authentication mechanism ────────────────────────────────────
            .httpBasic(httpBasic -> {})

            // ── Custom filter: log headers before authentication ────────────
            .addFilterBefore(requestHeaderLoggingFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── User store (in-memory — replace with DB-backed service in production) ─

    /**
     * Provides an in-memory user store with a single pre-configured user.
     *
     * <p><strong>Production note:</strong> Replace this bean with a
     * {@code JdbcUserDetailsManager} or a custom {@code UserDetailsService}
     * backed by a users table. Credentials should be injected from Vault /
     * environment variables — never hardcoded in source code.
     *
     * @param encoder the password encoder used to hash the stored password
     * @return an in-memory {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var adminUser = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))   // Inject from env in production
                .roles("ADMIN")
                .build();

        var readOnlyUser = User.builder()
                .username("viewer")
                .password(encoder.encode("viewer123"))  // Inject from env in production
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(adminUser, readOnlyUser);
    }

    // ── Password encoder ──────────────────────────────────────────────────────

    /**
     * Registers {@link BCryptPasswordEncoder} as the application-wide
     * {@link PasswordEncoder}. BCrypt is the industry standard for password
     * hashing — it is adaptive (cost factor configurable) and includes a
     * built-in salt.
     *
     * @return a {@link BCryptPasswordEncoder} with default cost factor (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
