package com.enterprise.employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (typically an {@code Employee}) does not
 * exist in the persistence store.
 *
 * <p>Maps to HTTP {@code 404 Not Found}.
 *
 * <p>Usage example:
 * <pre>{@code
 *   Employee entity = repository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
 * }</pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // ── Constants ────────────────────────────────────────────────────────────

    private static final String MESSAGE_TEMPLATE = "%s not found with %s: '%s'";

    // ── Fields ────────────────────────────────────────────────────────────────

    /** The name of the resource type that was not found (e.g., "Employee"). */
    private final String resourceName;

    /** The name of the field used to search (e.g., "id", "email"). */
    private final String fieldName;

    /** The value that was searched for. */
    private final Object fieldValue;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Creates a {@code ResourceNotFoundException} with a standardized message.
     *
     * @param resourceName the entity/resource type name
     * @param fieldName    the lookup field name
     * @param fieldValue   the value that could not be found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format(MESSAGE_TEMPLATE, resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
