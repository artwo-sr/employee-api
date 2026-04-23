package com.enterprise.employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule or invariant is violated — for example, when an
 * age value is inconsistent with the provided birth date, or when a duplicate
 * record is detected by business logic (not a DB constraint).
 *
 * <p>Maps to HTTP {@code 422 Unprocessable Entity}, which semantically means
 * "the request was well-formed but could not be processed due to business logic".
 *
 * <p>Usage example:
 * <pre>{@code
 *   if (calculatedAge != requestedAge) {
 *       throw new BusinessValidationException(
 *           "Age does not match the provided birth date");
 *   }
 * }</pre>
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessValidationException extends RuntimeException {

    /**
     * Creates a {@code BusinessValidationException} with the given detail message.
     *
     * @param message a human-readable explanation of the validation failure
     */
    public BusinessValidationException(String message) {
        super(message);
    }

    /**
     * Creates a {@code BusinessValidationException} with a detail message and
     * an underlying cause (e.g., wrapping a lower-level checked exception).
     *
     * @param message a human-readable explanation of the validation failure
     * @param cause   the original exception that triggered this failure
     */
    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
