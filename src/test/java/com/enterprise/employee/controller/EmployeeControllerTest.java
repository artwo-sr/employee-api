package com.enterprise.employee.controller;

import com.enterprise.employee.config.OpenApiConfig;
import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;
import com.enterprise.employee.entity.Sex;
import com.enterprise.employee.exception.ResourceNotFoundException;
import com.enterprise.employee.exception.handler.GlobalExceptionHandler;
import com.enterprise.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer (slice) tests for {@link EmployeeController}.
 *
 * <p>{@link WebMvcTest} loads only the controller and its MVC infrastructure
 * (filters, converters, {@link GlobalExceptionHandler}) — no database, no
 * service implementation, no Spring Security filter chain.
 *
 * <p>{@link AutoConfigureMockMvc} with {@code addFilters = false} disables the
 * full security filter chain so tests focus on controller logic without needing
 * to supply {@code Authorization} headers.
 *
 * <p>{@link Import} pulls in {@link GlobalExceptionHandler} so that validation
 * error scenarios (400 Bad Request) go through the advice and return the
 * standardised {@code ApiErrorResponse} shape.
 */
@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, OpenApiConfig.class})
@DisplayName("EmployeeController — Web Layer Tests")
class EmployeeControllerTest {

    // ── MockMvc (injected by @WebMvcTest) ─────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    // ── Mocked service ────────────────────────────────────────────────────────

    @MockBean
    private EmployeeService employeeService;

    // ── JSON serialiser ───────────────────────────────────────────────────────

    private ObjectMapper objectMapper;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private EmployeeResponseDTO sampleResponseDTO;
    private EmployeeRequestDTO  sampleRequestDTO;

    /**
     * Builds a dedicated {@link ObjectMapper} configured to handle Java 8 date/time
     * types ({@link LocalDate}, {@link LocalDateTime}) and to reproduce the same
     * serialisation behaviour as the running application.
     */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalDate birthDate = LocalDate.of(1994, 6, 15);

        sampleResponseDTO = EmployeeResponseDTO.builder()
                .id(1L)
                .firstName("John")
                .middleName("William")
                .fatherLastName("Doe")
                .motherLastName("Smith")
                .age(30)
                .sex(Sex.MALE)
                .birthDate(birthDate)
                .position("Software Engineer")
                .systemRegistrationDate(LocalDateTime.of(2026, 4, 20, 10, 0, 0))
                .active(true)
                .build();

        sampleRequestDTO = EmployeeRequestDTO.builder()
                .firstName("John")
                .middleName("William")
                .fatherLastName("Doe")
                .motherLastName("Smith")
                .age(30)
                .sex(Sex.MALE)
                .birthDate(birthDate)
                .position("Software Engineer")
                .active(true)
                .build();
    }

    // =========================================================================
    // GET /employees
    // =========================================================================

    @Nested
    @DisplayName("GET /employees")
    class GetAllEmployees {

        @Test
        @DisplayName("givenEmployeesExist_whenGetAll_thenReturn200AndList")
        void givenEmployeesExist_whenGetAll_thenReturn200AndList() throws Exception {
            // Given
            given(employeeService.getAllEmployees()).willReturn(List.of(sampleResponseDTO));

            // When / Then
            mockMvc.perform(get("/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].fatherLastName").value("Doe"));
        }
    }

    // =========================================================================
    // GET /employees/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /employees/{id}")
    class GetEmployeeById {

        @Test
        @DisplayName("givenExistingId_whenGetById_thenReturn200AndEmployeeDTO")
        void givenExistingId_whenGetById_thenReturn200AndEmployeeDTO() throws Exception {
            // Given
            given(employeeService.getEmployeeById(1L)).willReturn(sampleResponseDTO);

            // When / Then
            mockMvc.perform(get("/employees/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.fatherLastName").value("Doe"))
                    .andExpect(jsonPath("$.position").value("Software Engineer"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("givenNonExistingId_whenGetById_thenReturn404WithErrorBody")
        void givenNonExistingId_whenGetById_thenReturn404WithErrorBody() throws Exception {
            // Given
            given(employeeService.getEmployeeById(99L))
                    .willThrow(new ResourceNotFoundException("Employee", "id", 99L));

            // When / Then
            mockMvc.perform(get("/employees/99")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")))
                    .andExpect(jsonPath("$.path").value("/employees/99"));
        }

        @Test
        @DisplayName("givenNonNumericId_whenGetById_thenReturn400WithErrorBody")
        void givenNonNumericId_whenGetById_thenReturn400WithErrorBody() throws Exception {
            // When / Then — "abc" cannot be converted to Long
            mockMvc.perform(get("/employees/abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // =========================================================================
    // POST /employees
    // =========================================================================

    @Nested
    @DisplayName("POST /employees")
    class CreateEmployees {

        @Test
        @DisplayName("givenValidSinglePayload_whenCreate_thenReturn201AndCreatedDTO")
        void givenValidSinglePayload_whenCreate_thenReturn201AndCreatedDTO() throws Exception {
            // Given
            given(employeeService.createEmployees(anyList())).willReturn(List.of(sampleResponseDTO));

            String requestBody = objectMapper.writeValueAsString(List.of(sampleRequestDTO));

            // When / Then
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }

        @Test
        @DisplayName("givenValidBatchPayload_whenCreate_thenReturn201WithAllRecords")
        void givenValidBatchPayload_whenCreate_thenReturn201WithAllRecords() throws Exception {
            // Given
            EmployeeRequestDTO secondDTO = EmployeeRequestDTO.builder()
                    .firstName("Jane")
                    .fatherLastName("Roe")
                    .motherLastName("Brown")
                    .age(25)
                    .sex(Sex.FEMALE)
                    .birthDate(LocalDate.of(1999, 3, 22))
                    .position("QA Engineer")
                    .active(true)
                    .build();

            EmployeeResponseDTO secondResponse = EmployeeResponseDTO.builder()
                    .id(2L).firstName("Jane").fatherLastName("Roe")
                    .motherLastName("Brown").age(25).sex(Sex.FEMALE)
                    .birthDate(LocalDate.of(1999, 3, 22)).position("QA Engineer").active(true)
                    .build();

            given(employeeService.createEmployees(anyList()))
                    .willReturn(List.of(sampleResponseDTO, secondResponse));

            String requestBody = objectMapper.writeValueAsString(
                    List.of(sampleRequestDTO, secondDTO));

            // When / Then
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].firstName").value("Jane"));
        }

        @Test
        @DisplayName("givenMissingFirstName_whenCreate_thenReturn400WithFieldErrors")
        void givenMissingFirstName_whenCreate_thenReturn400WithFieldErrors() throws Exception {
            // Given — firstName is blank, triggering @NotBlank validation
            EmployeeRequestDTO invalidDTO = EmployeeRequestDTO.builder()
                    .firstName("")               // violates @NotBlank
                    .fatherLastName("Doe")
                    .motherLastName("Smith")
                    .age(30)
                    .sex(Sex.MALE)
                    .birthDate(LocalDate.of(1994, 6, 15))
                    .position("Engineer")
                    .active(true)
                    .build();

            String requestBody = objectMapper.writeValueAsString(List.of(invalidDTO));

            // When / Then
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[0].field")
                            .value(org.hamcrest.Matchers.containsString("firstName")));
        }

        @Test
        @DisplayName("givenMalformedBirthDate_whenCreate_thenReturn400")
        void givenMalformedBirthDate_whenCreate_thenReturn400() throws Exception {
            // Given — birthDate sent as yyyy-MM-dd; controller expects dd-MM-yyyy
            String requestBody = """
                    [
                      {
                        "firstName": "John",
                        "fatherLastName": "Doe",
                        "motherLastName": "Smith",
                        "age": 30,
                        "sex": "MALE",
                        "birthDate": "1994-06-15",
                        "position": "Engineer",
                        "active": true
                      }
                    ]
                    """;

            // When / Then
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("givenMissingFatherLastName_whenCreate_thenReturn400WithValidationError")
        void givenMissingFatherLastName_whenCreate_thenReturn400WithValidationError() throws Exception {
            // Given — fatherLastName is null, violating @NotBlank
            EmployeeRequestDTO invalidDTO = EmployeeRequestDTO.builder()
                    .firstName("John")
                    // fatherLastName intentionally omitted
                    .motherLastName("Smith")
                    .age(30)
                    .sex(Sex.MALE)
                    .birthDate(LocalDate.of(1994, 6, 15))
                    .position("Engineer")
                    .active(true)
                    .build();

            String requestBody = objectMapper.writeValueAsString(List.of(invalidDTO));

            // When / Then
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray());
        }
    }

    // =========================================================================
    // PUT /employees/{id}
    // =========================================================================

    @Nested
    @DisplayName("PUT /employees/{id}")
    class UpdateEmployee {

        @Test
        @DisplayName("givenExistingIdAndValidPayload_whenUpdate_thenReturn200AndUpdatedDTO")
        void givenExistingIdAndValidPayload_whenUpdate_thenReturn200AndUpdatedDTO() throws Exception {
            // Given
            given(employeeService.updateEmployee(eq(1L), any(EmployeeRequestDTO.class)))
                    .willReturn(sampleResponseDTO);

            String requestBody = objectMapper.writeValueAsString(sampleRequestDTO);

            // When / Then
            mockMvc.perform(put("/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        @DisplayName("givenNonExistingId_whenUpdate_thenReturn404")
        void givenNonExistingId_whenUpdate_thenReturn404() throws Exception {
            // Given
            given(employeeService.updateEmployee(eq(99L), any(EmployeeRequestDTO.class)))
                    .willThrow(new ResourceNotFoundException("Employee", "id", 99L));

            String requestBody = objectMapper.writeValueAsString(sampleRequestDTO);

            // When / Then
            mockMvc.perform(put("/employees/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // DELETE /employees/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /employees/{id}")
    class DeleteEmployee {

        @Test
        @DisplayName("givenExistingId_whenDelete_thenReturn204NoContent")
        void givenExistingId_whenDelete_thenReturn204NoContent() throws Exception {
            // Given
            willDoNothing().given(employeeService).deleteEmployee(1L);

            // When / Then
            mockMvc.perform(delete("/employees/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("givenNonExistingId_whenDelete_thenReturn404")
        void givenNonExistingId_whenDelete_thenReturn404() throws Exception {
            // Given
            willThrow(new ResourceNotFoundException("Employee", "id", 99L))
                    .given(employeeService).deleteEmployee(99L);

            // When / Then
            mockMvc.perform(delete("/employees/99")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // GET /employees/search
    // =========================================================================

    @Nested
    @DisplayName("GET /employees/search")
    class SearchByName {

        @Test
        @DisplayName("givenValidNameParam_whenSearch_thenReturn200AndMatchingList")
        void givenValidNameParam_whenSearch_thenReturn200AndMatchingList() throws Exception {
            // Given
            given(employeeService.searchByName("doe")).willReturn(List.of(sampleResponseDTO));

            // When / Then
            mockMvc.perform(get("/employees/search")
                            .queryParam("name", "doe")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].fatherLastName").value("Doe"));
        }

        @Test
        @DisplayName("givenNoMatch_whenSearch_thenReturn200AndEmptyList")
        void givenNoMatch_whenSearch_thenReturn200AndEmptyList() throws Exception {
            // Given
            given(employeeService.searchByName("xyz")).willReturn(List.of());

            // When / Then
            mockMvc.perform(get("/employees/search")
                            .queryParam("name", "xyz")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
