package com.enterprise.employee.service.impl;

import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;
import com.enterprise.employee.entity.Employee;
import com.enterprise.employee.exception.BusinessValidationException;
import com.enterprise.employee.exception.ResourceNotFoundException;
import com.enterprise.employee.mapper.EmployeeMapper;
import com.enterprise.employee.repository.EmployeeRepository;
import com.enterprise.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Implementation of {@link EmployeeService} containing all business logic for
 * the Employee management domain.
 *
 * <p>Architectural notes:
 * <ul>
 *   <li><strong>Constructor injection</strong> — all dependencies are injected via
 *       the constructor, enabling immutability and straightforward unit testing
 *       without a Spring context.</li>
 *   <li><strong>Transactional scope</strong> — read operations use
 *       {@code readOnly = true} to allow the JPA provider to apply optimisations
 *       (no dirty-checking, no flush). Write operations use the default propagation
 *       ({@code REQUIRED}) so all DB calls in one invocation join a single transaction.</li>
 *   <li><strong>Batch insert</strong> — {@link #createEmployees} delegates to
 *       {@code saveAll()}, which Hibernate will execute as a single batch when
 *       {@code hibernate.jdbc.batch_size} is configured in the prod profile. The
 *       entire list is committed or rolled back atomically.</li>
 *   <li><strong>Age consistency validation</strong> — on every create/update, the
 *       service cross-checks the stated {@code age} against the birth date to enforce
 *       data integrity beyond what Jakarta Validation can express declaratively.</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String EMPLOYEE_RESOURCE = "Employee";
    private static final String FIELD_ID          = "id";

    // ── Dependencies (constructor-injected) ───────────────────────────────────

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper     employeeMapper;

    /**
     * Primary constructor used by the Spring container.
     * No {@code @Autowired} annotation needed — Spring infers injection for
     * single-constructor beans automatically since Spring 4.3.
     *
     * @param employeeRepository the JPA repository for {@link Employee} entities
     * @param employeeMapper     the mapper component for entity ↔ DTO conversion
     */
    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper     = employeeMapper;
    }

    // ── Query operations (readOnly = true) ────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EmployeeResponseDTO> getAllEmployees() {
        log.info("Fetching all registered employees");

        List<Employee> employees = employeeRepository.findAll();

        log.info("Successfully retrieved {} employee(s)", employees.size());
        return employeeMapper.toResponseDTOList(employees);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmployeeResponseDTO getEmployeeById(Long id) {
        log.info("Fetching employee with ID: {}", id);

        Employee employee = findEmployeeOrThrow(id);

        log.info("Successfully retrieved employee with ID: {}", id);
        return employeeMapper.toResponseDTO(employee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EmployeeResponseDTO> searchByName(String name) {
        log.info("Searching employees by partial name: '{}'", name);

        List<Employee> results = employeeRepository.findByNameContainingIgnoreCase(name);

        log.info("Found {} employee(s) matching name '{}'", results.size(), name);
        return employeeMapper.toResponseDTOList(results);
    }

    // ── Mutating operations (readOnly = false, default REQUIRED propagation) ──

    /**
     * {@inheritDoc}
     *
     * <p>Every element in the list is validated for age consistency before any
     * record is persisted. This fail-fast approach prevents partial batch writes.
     */
    @Override
    @Transactional
    public List<EmployeeResponseDTO> createEmployees(List<EmployeeRequestDTO> requestDTOs) {
        log.info("Initiating batch insert of {} employee(s)", requestDTOs.size());

        requestDTOs.forEach(this::validateAgeConsistency);

        List<Employee> entities = employeeMapper.toEntityList(requestDTOs);
        List<Employee> saved    = employeeRepository.saveAll(entities);

        log.info("Successfully inserted {} employee(s)", saved.size());
        return employeeMapper.toResponseDTOList(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO requestDTO) {
        log.info("Updating employee with ID: {}", id);

        Employee existing = findEmployeeOrThrow(id);

        // Validate age/birth-date consistency against the effective (merged) values.
        // A partial update may supply only one of the two interdependent fields — for
        // example, the client changes only the position and omits both age and birthDate.
        // Without this merge, a null birthDate would silently skip the check, while a
        // null age would cause an NPE inside the validator. We therefore fall back to
        // the currently persisted value for whichever field the client did not send.
        if (requestDTO.getBirthDate() != null || requestDTO.getAge() != null) {
            LocalDate effectiveBirthDate =
                    requestDTO.getBirthDate() != null ? requestDTO.getBirthDate() : existing.getBirthDate();
            Integer effectiveAge =
                    requestDTO.getAge() != null ? requestDTO.getAge() : existing.getAge();
            validateAgeConsistency(effectiveBirthDate, effectiveAge);
        }

        employeeMapper.updateEntityFromDTO(requestDTO, existing);
        Employee updated = employeeRepository.save(existing);

        log.info("Successfully updated employee with ID: {}", id);
        return employeeMapper.toResponseDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with ID: {}", id);

        Employee existing = findEmployeeOrThrow(id);
        employeeRepository.delete(existing);

        log.info("Successfully deleted employee with ID: {}", id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Looks up an employee by primary key or throws {@link ResourceNotFoundException}.
     *
     * @param id the employee's primary key
     * @return the managed {@link Employee} entity
     */
    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_RESOURCE, FIELD_ID, id));
    }

    /**
     * Validates that the declared {@code age} is consistent with the {@code birthDate}.
     * The rule allows a tolerance of ±1 year to account for the current calendar year
     * (the employee may not have had their birthday yet this year).
     *
     * <p>This is a business-level validation that cannot be expressed with a single
     * Jakarta Validation annotation.
     *
     * @param dto the request DTO to validate
     * @throws BusinessValidationException if the age is inconsistent with the birth date
     */
    private void validateAgeConsistency(EmployeeRequestDTO dto) {
        if (dto.getBirthDate() != null && dto.getAge() != null) {
            validateAgeConsistency(dto.getBirthDate(), dto.getAge());
        }
    }

    /**
     * Core age consistency check.
     *
     * @param birthDate the employee's birth date
     * @param statedAge the age declared in the request
     * @throws BusinessValidationException if the discrepancy exceeds 1 year
     */
    private void validateAgeConsistency(LocalDate birthDate, Integer statedAge) {
        // Period.between counts completed years, which matches how humans express age
        // (a person born 15-Oct-1993 is still 31 until that date arrives each year).
        // A tolerance of ±1 year is intentional: it covers the window in which the
        // employee has not yet had their birthday in the current calendar year, making
        // a stated age of 31 valid when the calculated age is already 32.
        int calculatedAge = Period.between(birthDate, LocalDate.now()).getYears();
        int difference    = Math.abs(calculatedAge - statedAge);

        if (difference > 1) {
            throw new BusinessValidationException(
                    String.format(
                            "The stated age (%d) is inconsistent with the birth date (%s). "
                                    + "Calculated age is %d.",
                            statedAge, birthDate, calculatedAge));
        }
    }
}
