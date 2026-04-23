package com.enterprise.employee.exception.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized JSON error envelope returned by {@code GlobalExceptionHandler}
 * for every error condition.
 *
 * <p>Example JSON for a validation error:
 * <pre>{@code
 * {
 *   "timestamp": "20-04-2026 14:32:01",
 *   "status":    422,
 *   "error":     "Unprocessable Entity",
 *   "message":   "Validation failed for 2 field(s)",
 *   "path":      "/api/v1/employees",
 *   "fieldErrors": [
 *     { "field": "firstName", "rejectedValue": "", "message": "First name is required" },
 *     { "field": "age",       "rejectedValue": -1, "message": "Age must be at least 1" }
 *   ]
 * }
 * }</pre>
 *
 * <p>{@code fieldErrors} is omitted from the JSON body when null
 * (i.e., for non-validation errors), thanks to {@link JsonInclude}.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** Timestamp of when the error occurred. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private final LocalDateTime timestamp;

    /** HTTP status code (e.g., 404, 422, 500). */
    private final int status;

    /** Short HTTP status reason phrase (e.g., "Not Found", "Bad Request"). */
    private final String error;

    /** Human-readable description of the problem. */
    private final String message;

    /** The URI path that triggered the error. */
    private final String path;

    /**
     * Optional list of per-field validation errors; present only when the
     * error originates from {@code @Valid} / {@code @Validated} failures.
     */
    private final List<FieldValidationError> fieldErrors;

    // ── Nested type ──────────────────────────────────────────────────────────

    /**
     * Represents a single field-level constraint violation within
     * {@link ApiErrorResponse#fieldErrors}.
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldValidationError {

        /** The name of the DTO field that failed validation. */
        private final String field;

        /** The value that was submitted and rejected. */
        private final Object rejectedValue;

        /** The constraint violation message. */
        private final String message;
    }
}
