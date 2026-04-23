package com.enterprise.employee.mapper;

import com.enterprise.employee.dto.EmployeeRequestDTO;
import com.enterprise.employee.dto.EmployeeResponseDTO;
import com.enterprise.employee.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless mapping component responsible for translating between the
 * {@link Employee} JPA entity and its DTO representations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>{@link #toEntity(EmployeeRequestDTO)}           — DTO → new Entity (create path)</li>
 *   <li>{@link #toResponseDTO(Employee)}                — Entity → outbound DTO</li>
 *   <li>{@link #toEntityList(List)}                     — List of DTOs → List of Entities (batch create)</li>
 *   <li>{@link #toResponseDTOList(List)}                — List of Entities → List of outbound DTOs</li>
 *   <li>{@link #updateEntityFromDTO(EmployeeRequestDTO, Employee)} — partial/full update path</li>
 * </ul>
 *
 * <p>No frameworks (e.g. MapStruct) are used intentionally, to keep the mapping
 * logic explicit, testable, and free of annotation-processor complexity.
 */
@Component
public class EmployeeMapper {

    /**
     * Maps a validated {@link EmployeeRequestDTO} to a new {@link Employee} entity.
     * The {@code id} and {@code systemRegistrationDate} fields are intentionally
     * omitted — they are managed by the persistence layer.
     *
     * @param dto the inbound request DTO (must not be {@code null})
     * @return a new {@link Employee} instance ready to be persisted
     */
    public Employee toEntity(EmployeeRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return Employee.builder()
                .firstName(dto.getFirstName())
                .middleName(dto.getMiddleName())
                .fatherLastName(dto.getFatherLastName())
                .motherLastName(dto.getMotherLastName())
                .age(dto.getAge())
                .sex(dto.getSex())
                .birthDate(dto.getBirthDate())
                .position(dto.getPosition())
                .active(dto.getActive() != null ? dto.getActive() : Boolean.TRUE)
                .build();
    }

    /**
     * Maps a persisted {@link Employee} entity to an {@link EmployeeResponseDTO}.
     *
     * @param employee the entity (must not be {@code null})
     * @return a fully populated {@link EmployeeResponseDTO}
     */
    public EmployeeResponseDTO toResponseDTO(Employee employee) {
        if (employee == null) {
            return null;
        }
        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .middleName(employee.getMiddleName())
                .fatherLastName(employee.getFatherLastName())
                .motherLastName(employee.getMotherLastName())
                .age(employee.getAge())
                .sex(employee.getSex())
                .birthDate(employee.getBirthDate())
                .position(employee.getPosition())
                .systemRegistrationDate(employee.getSystemRegistrationDate())
                .active(employee.isActive())
                .build();
    }

    /**
     * Converts a list of {@link EmployeeRequestDTO} objects to a list of
     * {@link Employee} entities. Supports the batch-insert endpoint.
     *
     * @param dtos list of inbound request DTOs (must not be {@code null})
     * @return a list of new {@link Employee} instances
     */
    public List<Employee> toEntityList(List<EmployeeRequestDTO> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of {@link Employee} entities to a list of
     * {@link EmployeeResponseDTO} objects.
     *
     * @param employees list of persisted entities (must not be {@code null})
     * @return a list of outbound response DTOs
     */
    public List<EmployeeResponseDTO> toResponseDTOList(List<Employee> employees) {
        return employees.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Applies all non-null fields from a {@link EmployeeRequestDTO} onto an
     * existing {@link Employee} entity. This supports both full and partial
     * (PATCH-style) updates via the PUT endpoint.
     *
     * <p>Fields that are {@code null} in the DTO are left unchanged on the entity,
     * which means a client can omit a field to indicate "no change".
     *
     * @param dto      the inbound request DTO carrying the new values
     * @param employee the managed entity to be mutated in-place
     */
    public void updateEntityFromDTO(EmployeeRequestDTO dto, Employee employee) {
        if (dto.getFirstName() != null)      { employee.setFirstName(dto.getFirstName()); }
        if (dto.getMiddleName() != null)     { employee.setMiddleName(dto.getMiddleName()); }
        if (dto.getFatherLastName() != null) { employee.setFatherLastName(dto.getFatherLastName()); }
        if (dto.getMotherLastName() != null) { employee.setMotherLastName(dto.getMotherLastName()); }
        if (dto.getAge() != null)            { employee.setAge(dto.getAge()); }
        if (dto.getSex() != null)            { employee.setSex(dto.getSex()); }
        if (dto.getBirthDate() != null)      { employee.setBirthDate(dto.getBirthDate()); }
        if (dto.getPosition() != null)       { employee.setPosition(dto.getPosition()); }
        if (dto.getActive() != null)         { employee.setActive(dto.getActive()); }
    }
}
