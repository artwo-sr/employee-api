package com.enterprise.employee.controller;

import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;
import com.enterprise.employee.exception.response.ApiErrorResponse;
import com.enterprise.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing all {@code /employees} endpoints.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Receive and validate HTTP requests.</li>
 *   <li>Delegate all business logic to {@link EmployeeService}.</li>
 *   <li>Return semantically correct HTTP status codes via {@link ResponseEntity}.</li>
 * </ul>
 *
 * <p>This controller is deliberately thin — no business logic lives here.
 * It is the boundary between the HTTP transport layer and the service layer.
 *
 * <p>{@code @Validated} at class level activates constraint validation on
 * method parameters (e.g., the {@code @NotBlank} on the search {@code name}
 * query parameter).
 */
@Validated
@RestController
@RequestMapping(
        path     = "/employees",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Tag(
        name        = "Employee Management",
        description = "CRUD operations and name search for Employee records. "
                      + "Supports both single and batch inserts via the POST endpoint."
)
@SecurityRequirement(name = "basicAuth")
public class EmployeeController {

    // ── Dependency (constructor-injected) ─────────────────────────────────────

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── GET /employees ────────────────────────────────────────────────────────

    /**
     * Returns the full catalogue of registered employees with no filtering applied.
     * Delegates to {@link EmployeeService#getAllEmployees()} and wraps the result
     * in a standard {@code 200 OK} response envelope.
     *
     * @return {@link ResponseEntity} with HTTP {@code 200 OK} containing all employees;
     *         an empty JSON array is returned when no records exist
     */
    @Operation(
            summary     = "Retrieve all employees",
            description = "Returns a list of all registered employees. "
                          + "Returns an empty array if no employees are found."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Successfully retrieved all employees",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array     = @ArraySchema(schema = @Schema(implementation = EmployeeResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // ── GET /employees/{id} ───────────────────────────────────────────────────

    /**
     * Retrieves a single employee record by its primary-key {@code id}.
     *
     * @param id the unique numeric identifier of the employee; must be a positive {@link Long}
     * @return {@link ResponseEntity} with HTTP {@code 200 OK} containing the matching employee
     * @throws com.enterprise.employee.exception.ResourceNotFoundException propagated from the
     *         service layer when no employee exists for the given {@code id}, resulting in
     *         a {@code 404 Not Found} response via
     *         {@link com.enterprise.employee.exception.handler.GlobalExceptionHandler}
     */
    @Operation(
            summary     = "Retrieve an employee by ID",
            description = "Fetches a single employee record identified by its unique numeric ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Employee found and returned successfully",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = EmployeeResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Invalid ID format (non-numeric value supplied)",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description  = "No employee found with the given ID",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeById(
            @Parameter(description = "Unique numeric identifier of the employee", required = true, example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // ── POST /employees ───────────────────────────────────────────────────────

    /**
     * Persists one or more employees in a single, atomic database transaction.
     * Supplying a list of one element is semantically equivalent to a scalar create.
     *
     * <p>Validation is applied to every element before any record is written.
     * If any element fails Jakarta constraints or the age-consistency business rule,
     * the entire batch is rejected ({@code 400} or {@code 422} respectively) and
     * the database is left unchanged — no partial inserts.
     *
     * @param requestDTOs a non-empty JSON array of validated employee request objects
     * @return {@link ResponseEntity} with HTTP {@code 201 Created} containing all
     *         persisted employees in the same order as the input list
     */
    @Operation(
            summary     = "Create one or multiple employees (batch support)",
            description = "Accepts a JSON array of one or more employee objects and persists them "
                          + "in a single atomic transaction. If any element fails validation or a "
                          + "business rule, the entire batch is rejected and no records are created."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description  = "All employees created successfully",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array     = @ArraySchema(schema = @Schema(implementation = EmployeeResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Request body failed validation (field errors returned in response)",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description  = "Business rule violation (e.g., age inconsistent with birth date)",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EmployeeResponseDTO>> createEmployees(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Array of one or more employee objects to create",
                    required    = true,
                    content     = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array     = @ArraySchema(schema = @Schema(implementation = EmployeeRequestDTO.class))
                    )
            )
            @Valid @RequestBody List<@Valid EmployeeRequestDTO> requestDTOs) {

        List<EmployeeResponseDTO> created = employeeService.createEmployees(requestDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /employees/{id} ───────────────────────────────────────────────────

    /**
     * Updates the employee identified by {@code id}.
     * Fields that are {@code null} in the request body are not overwritten,
     * enabling partial updates — the client only needs to send the fields that changed.
     *
     * @param id         the primary key of the employee to update
     * @param requestDTO the DTO carrying new field values; only non-null fields are applied
     * @return {@link ResponseEntity} with HTTP {@code 200 OK} containing the updated employee
     * @throws com.enterprise.employee.exception.ResourceNotFoundException when no employee
     *         with the given {@code id} exists, resulting in {@code 404 Not Found}
     */
    @Operation(
            summary     = "Update an employee (full or partial)",
            description = "Updates an existing employee's fields. Only fields provided in the "
                          + "request body are updated; omitted (null) fields retain their current values. "
                          + "This enables both full (all fields supplied) and partial updates."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Employee updated successfully",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = EmployeeResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Request body failed validation",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description  = "No employee found with the given ID",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description  = "Business rule violation (e.g., age inconsistent with birth date)",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(
            @Parameter(description = "Unique numeric identifier of the employee to update", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {

        return ResponseEntity.ok(employeeService.updateEmployee(id, requestDTO));
    }

    // ── DELETE /employees/{id} ────────────────────────────────────────────────

    /**
     * Permanently removes the employee with the given {@code id} from the system.
     * This is a hard delete — there is no soft-delete or recycle-bin mechanism.
     *
     * @param id the primary key of the employee to delete
     * @return {@link ResponseEntity} with HTTP {@code 204 No Content}; no body is returned
     * @throws com.enterprise.employee.exception.ResourceNotFoundException when no employee
     *         with the given {@code id} exists, resulting in {@code 404 Not Found}
     */
    @Operation(
            summary     = "Delete an employee by ID",
            description = "Permanently removes the employee record with the specified ID from the system."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description  = "Employee deleted successfully — no content returned"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Invalid ID format",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description  = "No employee found with the given ID",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "Unique numeric identifier of the employee to delete", required = true, example = "1")
            @PathVariable Long id) {

        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /employees/search?name={name} ─────────────────────────────────────

    /**
     * Performs a case-insensitive, partial-match search across first name,
     * father's last name, and mother's last name simultaneously.
     *
     * <p>Implemented as a SQL {@code LIKE '%name%'} query. Accent-sensitive collation
     * behaviour depends on the database configuration (Oracle NLS settings in prod,
     * H2 defaults in dev).
     *
     * @param name the partial substring to search for; must not be blank
     * @return {@link ResponseEntity} with HTTP {@code 200 OK} containing all matching
     *         employees; returns an empty JSON array when no records match
     */
    @Operation(
            summary     = "Search employees by partial name",
            description = "Performs a case-insensitive partial search across first name, "
                          + "father's last name, and mother's last name. "
                          + "For example, searching 'garcia' matches 'García', 'GARCIA', and 'garcia'."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Search executed successfully (may return empty list)",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array     = @ArraySchema(schema = @Schema(implementation = EmployeeResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Missing or blank 'name' query parameter",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthorised — valid credentials required",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "Internal server error",
                    content      = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponseDTO>> searchByName(
            @Parameter(description = "Partial name string to search for (case-insensitive)", required = true, example = "garcia")
            @RequestParam @NotBlank(message = "Search parameter 'name' must not be blank") String name) {

        return ResponseEntity.ok(employeeService.searchByName(name));
    }
}
