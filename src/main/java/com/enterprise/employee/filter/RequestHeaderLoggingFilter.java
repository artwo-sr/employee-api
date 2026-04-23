package com.enterprise.employee.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringJoiner;

/**
 * Servlet filter that logs all incoming HTTP request headers before delegating
 * to the next element in the filter chain.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee exactly one execution
 * per request, even in forward/include dispatch scenarios.
 *
 * <p>Sensitive headers (e.g., {@code Authorization}) are detected and masked
 * so that credentials are never written to log files in plain text. This
 * addresses OWASP A09 — Security Logging and Monitoring Failures.
 *
 * <p>Log output example (level DEBUG):
 * <pre>
 * Incoming GET /api/v1/employees — Headers: [Accept: application/json]
 *   [Authorization:] [Content-Type: application/json]
 * </pre>
 */
@Slf4j
@Component
public class RequestHeaderLoggingFilter extends OncePerRequestFilter {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Placeholder written to the log in place of sensitive header values. */
    private static final String REDACTED = "****REDACTED****";

    /**
     * Lower-case header names whose values must never appear in logs.
     * Covers standard credential/token headers.
     */
    private static final java.util.Set<String> SENSITIVE_HEADERS = java.util.Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token"
    );

    // ── Filter logic ──────────────────────────────────────────────────────────

    /**
     * Logs all request headers (masking sensitive ones) then continues the
     * filter chain.
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the remaining filter chain
     */
    /**
     * Skips this filter for Swagger UI, OpenAPI spec, and Actuator paths.
     *
     * <p>Because this filter is annotated with {@code @Component}, Spring Boot
     * auto-registers it as a standalone Servlet filter (outside the Spring
     * Security chain). Without this override those paths would still be logged
     * even when {@code web.ignoring()} has bypassed the Security chain.
     *
     * @param request the current HTTP request
     * @return {@code true} if the filter should be skipped for this request
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Incoming {} {} — Headers: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    buildHeaderLog(request));
        }

        filterChain.doFilter(request, response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Iterates over all header names in the request and builds a single
     * bracketed, space-separated string for compact log output.
     *
     * @param request the current HTTP request
     * @return a formatted string representation of all headers
     */
    private String buildHeaderLog(HttpServletRequest request) {
        StringJoiner joiner = new StringJoiner(" ");

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return "(none)";
        }

        for (String headerName : Collections.list(headerNames)) {
            String displayValue = isSensitive(headerName)
                    ? REDACTED
                    : request.getHeader(headerName);
            joiner.add(String.format("[%s: %s]", headerName, displayValue));
        }

        return joiner.length() == 0 ? "(none)" : joiner.toString();
    }

    /**
     * Returns {@code true} if {@code headerName} is considered sensitive and
     * its value must be masked in logs.
     *
     * @param headerName the header name to check (case-insensitive)
     * @return {@code true} if the header value should be redacted
     */
    private boolean isSensitive(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }
}
