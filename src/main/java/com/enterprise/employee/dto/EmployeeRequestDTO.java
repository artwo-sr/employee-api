package com.enterprise.employee.dto;

import com.enterprise.employee.entity.Sex;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Inbound Data Transfer Object for Employee create/update operations.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>All mandatory fields are annotated with {@link NotBlank} or {@link NotNull}.</li>
 *   <li>{@code middleName} is deliberately optional — no {@link NotBlank} applied.</li>
 *   <li>{@code birthDate} must be a past date and is deserialized/serialized using
 *       the strictly enforced {@code dd-MM-yyyy} pattern.</li>
 *   <li>{@code active} defaults to {@code true}; clients may omit this field entirely
 *       on creation and it will be treated as active.</li>
 * </ul>
 *
 * <p><strong>Batch support:</strong> The POST endpoint accepts a
 * {@code List<EmployeeRequestDTO>}, so this class represents one unit in a batch.
 * A batch of a single element is semantically identical to a scalar request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequestDTO {

    // ─── NAME FIELDS ─────────────────────────────────────────────────────────

    @Schema(description = "Employee's first name", example = "Carlos")
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /** Optional middle name — validation only enforces the max length when present. */
    @Schema(description = "Employee's middle name — optional, omit when not applicable", example = "Alberto")
    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    private String middleName;

    @Schema(description = "Employee's paternal (father's) last name", example = "Garc\u00eda")
    @NotBlank(message = "Father's last name is required")
    @Size(max = 100, message = "Father's last name must not exceed 100 characters")
    private String fatherLastName;

    @Schema(description = "Employee's maternal (mother's) last name", example = "L\u00f3pez")
    @NotBlank(message = "Mother's last name is required")
    @Size(max = 100, message = "Mother's last name must not exceed 100 characters")
    private String motherLastName;

    // ─── PERSONAL DATA ───────────────────────────────────────────────────────

    @Schema(description = "Age in years; must be consistent with birthDate within a \u00b11 year tolerance", example = "32")
    @NotNull(message = "Age is required")
    @Min(value = 1,   message = "Age must be at least 1")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;

    @Schema(description = "Employee's biological sex", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    @NotNull(message = "Sex is required (MALE, FEMALE, OTHER)")
    private Sex sex;

    /**
     * Birth date strictly enforced as {@code dd-MM-yyyy} both on deserialization
     * (JSON → Java) and on serialization (Java → JSON).
     * Must reference a date in the past; today's date is rejected by {@link Past}.
     */
    @Schema(description = "Employee's birth date; must be a past date", example = "10-03-1993")
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be a date in the past")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    // ─── JOB INFORMATION ─────────────────────────────────────────────────────

    @Schema(description = "Employee's job title or position within the organisation", example = "Software Engineer")
    @NotBlank(message = "Position / job title is required")
    @Size(max = 150, message = "Position must not exceed 150 characters")
    private String position;

    // ─── FLAGS ───────────────────────────────────────────────────────────────

    /**
     * Active status. When the client omits this field the Java default is {@code null};
     * the mapper layer treats {@code null} as {@code true} (active by default).
     */
    @Schema(description = "Whether the employee record is active; omit to default to true", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean active = Boolean.TRUE;
}
