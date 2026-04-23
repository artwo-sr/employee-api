package com.enterprise.employee.service.impl;

import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;
import com.enterprise.employee.entity.Employee;
import com.enterprise.employee.entity.Sex;
import com.enterprise.employee.exception.BusinessValidationException;
import com.enterprise.employee.exception.ResourceNotFoundException;
import com.enterprise.employee.mapper.EmployeeMapper;
import com.enterprise.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link EmployeeServiceImpl}.
 *
 * <p>Uses {@link MockitoExtension} — no Spring context is loaded, keeping
 * tests fast and fully isolated. All collaborators ({@link EmployeeRepository}
 * and {@link EmployeeMapper}) are replaced with Mockito mocks.
 *
 * <p>Test structure follows BDD Given-When-Then naming and {@link Nested}
 * classes grouped by the operation under test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl — Unit Tests")
class EmployeeServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    // ── Subject under test ────────────────────────────────────────────────────

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    // ── Shared fixtures ─────────────────────────────────────────────────────

    private Employee sampleEmployee;
    private EmployeeResponseDTO sampleResponseDTO;
    private EmployeeRequestDTO sampleRequestDTO;

    /**
     * Initialises shared test fixtures before each test method.
     * Using {@code LocalDate.now().minusYears(30)} ensures age-consistency
     * validation always passes, regardless of the date the tests run.
     */
    @BeforeEach
    void setUp() {
        LocalDate birthDate = LocalDate.now().minusYears(30);

        sampleEmployee = Employee.builder()
                .id(1L)
                .firstName("John")
                .middleName("William")
                .fatherLastName("Doe")
                .motherLastName("Smith")
                .age(30)
                .sex(Sex.MALE)
                .birthDate(birthDate)
                .position("Software Engineer")
                .systemRegistrationDate(LocalDateTime.now())
                .active(true)
                .build();

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
                .systemRegistrationDate(sampleEmployee.getSystemRegistrationDate())
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
    // getAllEmployees
    // =========================================================================

    @Nested
    @DisplayName("getAllEmployees()")
    class GetAllEmployees {

        @Test
        @DisplayName("givenEmployeesExist_whenGetAll_thenReturnListOfDTOs")
        void givenEmployeesExist_whenGetAll_thenReturnListOfDTOs() {
            // Given
            given(employeeRepository.findAll()).willReturn(List.of(sampleEmployee));
            given(employeeMapper.toResponseDTOList(List.of(sampleEmployee)))
                    .willReturn(List.of(sampleResponseDTO));

            // When
            List<EmployeeResponseDTO> result = employeeService.getAllEmployees();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFirstName()).isEqualTo("John");
            then(employeeRepository).should(times(1)).findAll();
        }

        @Test
        @DisplayName("givenNoEmployeesExist_whenGetAll_thenReturnEmptyList")
        void givenNoEmployeesExist_whenGetAll_thenReturnEmptyList() {
            // Given
            given(employeeRepository.findAll()).willReturn(List.of());
            given(employeeMapper.toResponseDTOList(List.of())).willReturn(List.of());

            // When
            List<EmployeeResponseDTO> result = employeeService.getAllEmployees();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getEmployeeById
    // =========================================================================

    @Nested
    @DisplayName("getEmployeeById()")
    class GetEmployeeById {

        @Test
        @DisplayName("givenExistingId_whenGetById_thenReturnEmployeeDTO")
        void givenExistingId_whenGetById_thenReturnEmployeeDTO() {
            // Given
            given(employeeRepository.findById(1L)).willReturn(Optional.of(sampleEmployee));
            given(employeeMapper.toResponseDTO(sampleEmployee)).willReturn(sampleResponseDTO);

            // When
            EmployeeResponseDTO result = employeeService.getEmployeeById(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L,  result.getId());
            assertEquals("John", result.getFirstName());
            assertEquals("Doe",  result.getFatherLastName());
            then(employeeRepository).should(times(1)).findById(1L);
        }

        @Test
        @DisplayName("givenNonExistingId_whenGetById_thenThrowResourceNotFoundException")
        void givenNonExistingId_whenGetById_thenThrowResourceNotFoundException() {
            // Given
            given(employeeRepository.findById(99L)).willReturn(Optional.empty());

            // When / Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> employeeService.getEmployeeById(99L)
            );

            assertThat(exception.getMessage()).contains("Employee");
            assertThat(exception.getMessage()).contains("99");
            then(employeeRepository).should(times(1)).findById(99L);
            then(employeeMapper).should(never()).toResponseDTO(any());
        }
    }

    // =========================================================================
    // createEmployees (batch)
    // =========================================================================

    @Nested
    @DisplayName("createEmployees()")
    class CreateEmployees {

        @Test
        @DisplayName("givenValidSingleRequest_whenCreate_thenSaveAllCalledAndReturnDTO")
        void givenValidSingleRequest_whenCreate_thenSaveAllCalledAndReturnDTO() {
            // Given
            given(employeeMapper.toEntityList(List.of(sampleRequestDTO)))
                    .willReturn(List.of(sampleEmployee));
            given(employeeRepository.saveAll(List.of(sampleEmployee)))
                    .willReturn(List.of(sampleEmployee));
            given(employeeMapper.toResponseDTOList(List.of(sampleEmployee)))
                    .willReturn(List.of(sampleResponseDTO));

            // When
            List<EmployeeResponseDTO> result = employeeService.createEmployees(List.of(sampleRequestDTO));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFirstName()).isEqualTo("John");
            then(employeeRepository).should(times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("givenValidBatchRequest_whenCreate_thenSaveAllCalledOnceForEntireBatch")
        void givenValidBatchRequest_whenCreate_thenSaveAllCalledOnceForEntireBatch() {
            // Given
            LocalDate birthDate2 = LocalDate.now().minusYears(25);
            EmployeeRequestDTO secondDTO = EmployeeRequestDTO.builder()
                    .firstName("Jane")
                    .fatherLastName("Roe")
                    .motherLastName("Brown")
                    .age(25)
                    .sex(Sex.FEMALE)
                    .birthDate(birthDate2)
                    .position("QA Engineer")
                    .active(true)
                    .build();

            Employee secondEmployee = Employee.builder()
                    .id(2L).firstName("Jane").fatherLastName("Roe")
                    .motherLastName("Brown").age(25).sex(Sex.FEMALE)
                    .birthDate(birthDate2).position("QA Engineer").active(true)
                    .build();

            EmployeeResponseDTO secondResponse = EmployeeResponseDTO.builder()
                    .id(2L).firstName("Jane").fatherLastName("Roe")
                    .motherLastName("Brown").age(25).sex(Sex.FEMALE)
                    .birthDate(birthDate2).position("QA Engineer").active(true)
                    .build();

            List<EmployeeRequestDTO> batch     = List.of(sampleRequestDTO, secondDTO);
            List<Employee>           entities  = List.of(sampleEmployee, secondEmployee);
            List<EmployeeResponseDTO> responses = List.of(sampleResponseDTO, secondResponse);

            given(employeeMapper.toEntityList(batch)).willReturn(entities);
            given(employeeRepository.saveAll(entities)).willReturn(entities);
            given(employeeMapper.toResponseDTOList(entities)).willReturn(responses);

            // When
            List<EmployeeResponseDTO> result = employeeService.createEmployees(batch);

            // Then
            assertThat(result).hasSize(2);
            // saveAll must be invoked exactly once for atomicity — not once per element
            then(employeeRepository).should(times(1)).saveAll(entities);
        }

        @Test
        @DisplayName("givenAgeInconsistentWithBirthDate_whenCreate_thenThrowBusinessValidationException")
        void givenAgeInconsistentWithBirthDate_whenCreate_thenThrowBusinessValidationException() {
            // Given — birth date implies age 30, but stated age is 55 (>1 yr delta)
            EmployeeRequestDTO invalidDTO = EmployeeRequestDTO.builder()
                    .firstName("Carlos")
                    .fatherLastName("Martinez")
                    .motherLastName("Lopez")
                    .age(55)
                    .sex(Sex.MALE)
                    .birthDate(LocalDate.now().minusYears(30))
                    .position("Manager")
                    .active(true)
                    .build();

            // When / Then
            assertThatThrownBy(() -> employeeService.createEmployees(List.of(invalidDTO)))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("inconsistent with the birth date");

            then(employeeRepository).should(never()).saveAll(anyList());
        }
    }

    // =========================================================================
    // updateEmployee
    // =========================================================================

    @Nested
    @DisplayName("updateEmployee()")
    class UpdateEmployee {

        @Test
        @DisplayName("givenExistingIdAndFullRequest_whenUpdate_thenAllFieldsUpdated")
        void givenExistingIdAndFullRequest_whenUpdate_thenAllFieldsUpdated() {
            // Given
            given(employeeRepository.findById(1L)).willReturn(Optional.of(sampleEmployee));
            given(employeeRepository.save(sampleEmployee)).willReturn(sampleEmployee);
            given(employeeMapper.toResponseDTO(sampleEmployee)).willReturn(sampleResponseDTO);
            // mapper.updateEntityFromDTO is a void method — no stubbing needed; Mockito
            // uses a no-op by default, which is correct here: we verify via side effects.

            // When
            EmployeeResponseDTO result = employeeService.updateEmployee(1L, sampleRequestDTO);

            // Then
            assertNotNull(result);
            then(employeeRepository).should(times(1)).findById(1L);
            then(employeeMapper).should(times(1))
                    .updateEntityFromDTO(sampleRequestDTO, sampleEmployee);
            then(employeeRepository).should(times(1)).save(sampleEmployee);
        }

        @Test
        @DisplayName("givenPartialRequest_whenUpdate_thenMapperAppliesOnlyNonNullFields")
        void givenPartialRequest_whenUpdate_thenMapperAppliesOnlyNonNullFields() {
            // Given — only position is changed; all other fields are null (partial update)
            EmployeeRequestDTO partialDTO = EmployeeRequestDTO.builder()
                    .position("Senior Software Engineer")
                    .build();

            given(employeeRepository.findById(1L)).willReturn(Optional.of(sampleEmployee));
            given(employeeRepository.save(sampleEmployee)).willReturn(sampleEmployee);
            given(employeeMapper.toResponseDTO(sampleEmployee)).willReturn(sampleResponseDTO);

            // When
            employeeService.updateEmployee(1L, partialDTO);

            // Then — mapper must be called so it can apply the partial changes
            then(employeeMapper).should(times(1))
                    .updateEntityFromDTO(partialDTO, sampleEmployee);
            // The original entity is passed to save(), not a new one
            then(employeeRepository).should(times(1)).save(sampleEmployee);
        }

        @Test
        @DisplayName("givenNonExistingId_whenUpdate_thenThrowResourceNotFoundException")
        void givenNonExistingId_whenUpdate_thenThrowResourceNotFoundException() {
            // Given
            given(employeeRepository.findById(99L)).willReturn(Optional.empty());

            // When / Then
            assertThrows(ResourceNotFoundException.class,
                    () -> employeeService.updateEmployee(99L, sampleRequestDTO));

            then(employeeMapper).should(never()).updateEntityFromDTO(any(), any());
            then(employeeRepository).should(never()).save(any());
        }
    }

    // =========================================================================
    // deleteEmployee
    // =========================================================================

    @Nested
    @DisplayName("deleteEmployee()")
    class DeleteEmployee {

        @Test
        @DisplayName("givenExistingId_whenDelete_thenRepositoryDeleteIsCalled")
        void givenExistingId_whenDelete_thenRepositoryDeleteIsCalled() {
            // Given
            given(employeeRepository.findById(1L)).willReturn(Optional.of(sampleEmployee));
            willDoNothing().given(employeeRepository).delete(sampleEmployee);

            // When
            employeeService.deleteEmployee(1L);

            // Then
            then(employeeRepository).should(times(1)).delete(sampleEmployee);
        }

        @Test
        @DisplayName("givenNonExistingId_whenDelete_thenThrowResourceNotFoundException")
        void givenNonExistingId_whenDelete_thenThrowResourceNotFoundException() {
            // Given
            given(employeeRepository.findById(99L)).willReturn(Optional.empty());

            // When / Then
            assertThrows(ResourceNotFoundException.class,
                    () -> employeeService.deleteEmployee(99L));

            then(employeeRepository).should(never()).delete(any());
        }
    }

    // =========================================================================
    // searchByName
    // =========================================================================

    @Nested
    @DisplayName("searchByName()")
    class SearchByName {

        @Test
        @DisplayName("givenPartialName_whenSearch_thenReturnMatchingEmployees")
        void givenPartialName_whenSearch_thenReturnMatchingEmployees() {
            // Given
            given(employeeRepository.findByNameContainingIgnoreCase("doe"))
                    .willReturn(List.of(sampleEmployee));
            given(employeeMapper.toResponseDTOList(List.of(sampleEmployee)))
                    .willReturn(List.of(sampleResponseDTO));

            // When
            List<EmployeeResponseDTO> result = employeeService.searchByName("doe");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFatherLastName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("givenNoMatch_whenSearch_thenReturnEmptyList")
        void givenNoMatch_whenSearch_thenReturnEmptyList() {
            // Given
            given(employeeRepository.findByNameContainingIgnoreCase("xyz"))
                    .willReturn(List.of());
            given(employeeMapper.toResponseDTOList(List.of())).willReturn(List.of());

            // When
            List<EmployeeResponseDTO> result = employeeService.searchByName("xyz");

            // Then
            assertThat(result).isEmpty();
        }
    }
}
