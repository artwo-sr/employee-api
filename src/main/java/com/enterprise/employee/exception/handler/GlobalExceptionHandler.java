package com.enterprise.employee.exception.handler;

import com.enterprise.employee.exception.BusinessValidationException;
import com.enterprise.employee.exception.ResourceNotFoundException;
import com.enterprise.employee.exception.response.ApiErrorResponse;
import com.enterprise.employee.exception.response.ApiErrorResponse.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception-handling component that intercepts all unhandled
 * exceptions thrown from {@code @RestController} beans.
 *
 * <p>Every handler method builds an {@link ApiErrorResponse} and returns it
 * wrapped in a {@link ResponseEntity} with the correct HTTP status code so that
 * the client always receives a consistent JSON error envelope — never a raw
 * Spring WhiteLabel error page.
 *
 * <p>Handler priority (Spring resolves the most specific match first):
 * <ol>
 *   <li>{@link ResourceNotFoundException}              → 404</li>
 *   <li>{@link BusinessValidationException}            → 422</li>
 *   <li>{@link MethodArgumentNotValidException}         → 400 + field errors (Spring MVC)</li>
 *   <li>{@link ConstraintViolationException}            → 400 + field errors (Bean Validation AOP)</li>
 *   <li>{@link HttpMessageNotReadableException}         → 400 (malformed JSON)</li>
 *   <li>{@link MissingServletRequestParameterException} → 400</li>
 *   <li>{@link MethodArgumentTypeMismatchException}     → 400 (wrong type in path/query)</li>
 *   <li>{@link NoResourceFoundException}               → 404 (Spring MVC 6.1+)</li>
 *   <li>{@link Exception} (catch-all)                  → 500</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String LOG_PREFIX_NOT_FOUND  = "Resource not found: {}";
    private static final String LOG_PREFIX_BUSINESS   = "Business validation error: {}";
    private static final String LOG_PREFIX_VALIDATION = "Request validation failed on path [{}]: {}";
    private static final String LOG_PREFIX_UNREADABLE = "Malformed JSON request on path [{}]: {}";
    private static final String LOG_PREFIX_UNEXPECTED = "Unexpected error on path [{}]";

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    /**
     * Handles {@link ResourceNotFoundException} — emitted by the service when a
     * requested entity ID does not exist.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn(LOG_PREFIX_NOT_FOUND, ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // ── 422 Unprocessable Entity ───────────────────────────────────────────────

    /**
     * Handles {@link BusinessValidationException} — emitted when business logic
     * rejects a semantically invalid request (e.g., age/birth-date mismatch).
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessValidation(
            BusinessValidationException ex,
            HttpServletRequest request) {

        log.warn(LOG_PREFIX_BUSINESS, ex.getMessage());

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // ── 400 Bad Request — DTO constraint violations ────────────────────────────

    /**
     * Handles {@link MethodArgumentNotValidException} — thrown by Spring when a
     * {@code @Valid}-annotated request body fails jakarta.validation constraints.
     * All per-field errors are collected and returned in the {@code fieldErrors} array.
     *
     * <p>Also covers nested list validation (batch insert), because Spring validates
     * each element in the {@code List<EmployeeRequestDTO>} and aggregates failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> FieldValidationError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        String summary = String.format("Validation failed for %d field(s)", fieldErrors.size());
        log.warn(LOG_PREFIX_VALIDATION, request.getRequestURI(), summary);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                summary,
                request.getRequestURI(),
                fieldErrors
        );
    }
    // ── 400 Bad Request — Bean Validation AOP method-level violations ──────────

    /**
     * Handles {@link ConstraintViolationException} — thrown by the Bean Validation
     * AOP interceptor ({@code MethodValidationInterceptor}) when the controller class
     * carries {@code @Validated} AND method parameters violate their constraints.
     *
     * <p>This is distinct from {@link MethodArgumentNotValidException}, which is
     * raised by Spring MVC's argument resolver for {@code @RequestBody} objects.
     * Both exist because {@code @Validated} at the class level enables an AOP proxy
     * that can intercept and validate method arguments independently of the MVC pipeline.
     *
     * <p>The property path in a {@link jakarta.validation.ConstraintViolation} uses
     * the format {@code methodName.paramName[index].fieldName}. Only the last
     * path node (the actual field name) is exposed to the client.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    // Property path: "methodName.paramName[0].fieldName"
                    // Strip everything up to and including the last dot to get
                    // the plain field name that the client can act on.
                    String fullPath  = cv.getPropertyPath().toString();
                    String fieldName = fullPath.contains(".")
                            ? fullPath.substring(fullPath.lastIndexOf('.') + 1)
                            : fullPath;
                    return FieldValidationError.builder()
                            .field(fieldName)
                            .rejectedValue(cv.getInvalidValue())
                            .message(cv.getMessage())
                            .build();
                })
                .collect(Collectors.toList());

        String summary = String.format("Validation failed for %d field(s)", fieldErrors.size());
        log.warn(LOG_PREFIX_VALIDATION, request.getRequestURI(), summary);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                summary,
                request.getRequestURI(),
                fieldErrors
        );
    }
    // ── 400 Bad Request — malformed JSON body ─────────────────────────────────

    /**
     * Handles {@link HttpMessageNotReadableException} — thrown when the request
     * body cannot be parsed (e.g., invalid JSON syntax, wrong date format).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn(LOG_PREFIX_UNREADABLE, request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request or invalid field format. "
                        + "Ensure dates follow the pattern dd-MM-yyyy.",
                request.getRequestURI(),
                null
        );
    }

    // ── 400 Bad Request — missing query parameter ─────────────────────────────

    /**
     * Handles {@link MissingServletRequestParameterException} — thrown when a
     * required {@code @RequestParam} is absent (e.g., missing {@code name} on
     * {@code GET /employees/search}).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = String.format("Required request parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        log.warn("Missing request parameter on path [{}]: {}", request.getRequestURI(), message);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    // ── 400 Bad Request — path/query variable type mismatch ───────────────────

    /**
     * Handles {@link MethodArgumentTypeMismatchException} — thrown when a path
     * variable or query parameter cannot be converted (e.g., {@code /employees/abc}
     * when the method expects a {@code Long}).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String expectedType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";

        String message = String.format(
                "Parameter '%s' has an invalid value '%s'. Expected type: %s",
                ex.getName(), ex.getValue(), expectedType);

        log.warn("Type mismatch on path [{}]: {}", request.getRequestURI(), message);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                null
        );
    }

    // ── 404 Not Found — no static resource (Spring MVC internal) ──────────────

    /**
     * Handles {@link NoResourceFoundException} — thrown by Spring MVC in Spring 6.1+
     * when no handler and no static resource is found for a given path.
     *
     * <p>This handler must be explicit so that the generic {@link Exception} catch-all
     * does not intercept it and incorrectly return a 500 response. Returning 404
     * here preserves the correct HTTP semantics and allows normal Spring resource
     * resolution to proceed without interference.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.warn("No resource found for path [{}]: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // ── 500 Internal Server Error — catch-all ─────────────────────────────────

    /**
     * Catch-all handler for any unhandled {@link Exception}. Logs the full stack
     * trace at ERROR level (important for monitoring/alerting) but returns only a
     * generic message to the client to avoid leaking implementation details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error(LOG_PREFIX_UNEXPECTED, request.getRequestURI(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact support.",
                request.getRequestURI(),
                null
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a {@link ResponseEntity} wrapping an {@link ApiErrorResponse} with
     * all common fields populated.
     *
     * @param status      the HTTP status to set on the response
     * @param message     the human-readable error message
     * @param path        the request URI that triggered the error
     * @param fieldErrors optional list of field-level validation errors
     * @return a fully constructed {@link ResponseEntity}
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<FieldValidationError> fieldErrors) {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(body, status);
    }
}
