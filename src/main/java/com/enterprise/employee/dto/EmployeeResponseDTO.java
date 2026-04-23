package com.enterprise.employee.dto;

import com.enterprise.employee.entity.Sex;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outbound Data Transfer Object returned by all Employee endpoints.
 *
 * <p>No validation annotations are placed here — this object is strictly write-only
 * from the API perspective (it is never deserialized from client input).
 *
 * <p>Date formatting:
 * <ul>
 *   <li>{@code birthDate} — serialized as {@code dd-MM-yyyy} to match the input contract.</li>
 *   <li>{@code systemRegistrationDate} — serialized with full timestamp for auditability.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponseDTO {

    @Schema(description = "Unique system-assigned employee identifier", example = "1")
    private Long id;

    // ─── NAME FIELDS ─────────────────────────────────────────────────

    @Schema(description = "Employee's first name", example = "Carlos")
    private String firstName;

    @Schema(description = "Employee's middle name", example = "Alberto")
    private String middleName;

    @Schema(description = "Employee's paternal last name", example = "Garc\u00eda")
    private String fatherLastName;

    @Schema(description = "Employee's maternal last name", example = "L\u00f3pez")
    private String motherLastName;

    // ─── PERSONAL DATA ───────────────────────────────────────────────────────

    @Schema(description = "Age in years", example = "32")
    private Integer age;

    @Schema(description = "Employee's biological sex", example = "MALE")
    private Sex sex;

    @Schema(description = "Birth date in dd-MM-yyyy format", example = "10-03-1993")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    // ─── JOB INFORMATION ─────────────────────────────────────────────────────

    @Schema(description = "Employee's job title or position", example = "Software Engineer")
    private String position;

    // ─── AUDIT / FLAGS ───────────────────────────────────────────────

    @Schema(description = "UTC timestamp when the record was first inserted (system-managed, non-updatable)", example = "20-04-2026 10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime systemRegistrationDate;

    @Schema(description = "Whether the employee record is currently active", example = "true")
    private boolean active;
}
