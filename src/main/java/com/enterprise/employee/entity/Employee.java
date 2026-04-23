package com.enterprise.employee.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity representing an Employee in the system.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code systemRegistrationDate} is managed exclusively by Hibernate via
 *       {@link CreationTimestamp} and is never updatable.</li>
 *   <li>{@code active} defaults to {@code true} — the {@code @Builder.Default}
 *       annotation prevents Lombok's builder from zeroing out the primitive default.</li>
 *   <li>{@code sex} is persisted as a {@link EnumType#STRING} to remain readable
 *       in the DB and survive enum reorderings.</li>
 *   <li>A SEQUENCE strategy is used for ID generation to be compatible with both
 *       H2 (dev/test profiles) and Oracle (prod profile).</li>
 * </ul>
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(
            name       = "employee_seq",
            sequenceName = "employee_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    // ─── NAME FIELDS ─────────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Optional — not all employees have a registered middle name. */
    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "father_last_name", nullable = false, length = 100)
    private String fatherLastName;

    @Column(name = "mother_last_name", nullable = false, length = 100)
    private String motherLastName;

    // ─── PERSONAL DATA ───────────────────────────────────────────────────────

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    /**
     * Employee's birth date stored as a plain calendar date (no time zone).
     * Serialization format {@code dd-MM-yyyy} is enforced at the DTO layer.
     */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    // ─── JOB INFORMATION ─────────────────────────────────────────────────────

    /** Position / job title within the organisation. */
    @Column(name = "position", nullable = false, length = 150)
    private String position;

    // ─── AUDIT / FLAGS ───────────────────────────────────────────────────────

    /**
     * Timestamp set once at INSERT time by Hibernate. The {@code updatable = false}
     * constraint prevents accidental overwrites via JPQL bulk updates.
     */
    @CreationTimestamp
    @Column(name = "system_registration_date", nullable = false, updatable = false)
    private LocalDateTime systemRegistrationDate;

    /**
     * Soft-delete / activation flag. Defaults to {@code true} so that every newly
     * created employee is immediately active. {@code @Builder.Default} ensures the
     * Lombok builder respects this value instead of defaulting the primitive to false.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
