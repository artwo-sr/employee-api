package com.enterprise.employee.service;

import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;

import java.util.List;

/**
 * Service contract defining all business operations for {@code Employee} management.
 *
 * <p>This interface decouples the controller from the implementation class,
 * enabling independent testing via mocks and making it straightforward to
 * swap the implementation (e.g., caching decorator, read-replica variant).
 *
 * <p>All batch operations (e.g., {@link #createEmployees}) share a single
 * transactional boundary to guarantee atomicity.
 */
public interface EmployeeService {

    /**
     * Retrieves a list of all registered employees.
     *
     * @return a list of {@link EmployeeResponseDTO}; empty list if none exist
     */
    List<EmployeeResponseDTO> getAllEmployees();

    /**
     * Retrieves a single employee by their unique identifier.
     *
     * @param id the employee's primary key
     * @return the matching {@link EmployeeResponseDTO}
     * @throws com.enterprise.employee.exception.ResourceNotFoundException
     *         if no employee with the given {@code id} exists
     */
    EmployeeResponseDTO getEmployeeById(Long id);

    /**
     * Creates one or more employees in a single, atomic operation.
     *
     * <p>A list of one element is semantically equivalent to a single insert.
     * All inserts in the list share the same transaction — if any record fails,
     * the entire batch is rolled back.
     *
     * @param requestDTOs a non-empty list of validated employee request DTOs
     * @return a list of {@link EmployeeResponseDTO} for every persisted record,
     *         in the same order as the input list
     */
    List<EmployeeResponseDTO> createEmployees(List<EmployeeRequestDTO> requestDTOs);

    /**
     * Updates an existing employee, supporting both full and partial updates.
     *
     * <p>Only fields that are non-null in the {@code requestDTO} are overwritten;
     * absent (null) fields retain their current persisted value.
     *
     * @param id         the primary key of the employee to update
     * @param requestDTO the DTO carrying the new field values
     * @return the updated employee as an {@link EmployeeResponseDTO}
     * @throws com.enterprise.employee.exception.ResourceNotFoundException
     *         if no employee with the given {@code id} exists
     */
    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO requestDTO);

    /**
     * Permanently removes an employee record from the database.
     *
     * @param id the primary key of the employee to delete
     * @throws com.enterprise.employee.exception.ResourceNotFoundException
     *         if no employee with the given {@code id} exists
     */
    void deleteEmployee(Long id);

    /**
     * Returns all employees whose first name, father's last name, or mother's last
     * name contains the {@code name} substring (case-insensitive partial match).
     *
     * @param name the partial name string to search for; must not be blank
     * @return a list of matching {@link EmployeeResponseDTO}; empty list if none match
     */
    List<EmployeeResponseDTO> searchByName(String name);
}
