package com.enterprise.employee.repository;

import com.enterprise.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Employee} entities.
 *
 * <p>Extends {@link JpaRepository} which provides standard CRUD operations:
 * {@code save}, {@code saveAll}, {@code findById}, {@code findAll},
 * {@code deleteById}, {@code existsById}, etc.
 *
 * <p>Custom queries are defined using JPQL to remain database-agnostic
 * (compatible with both H2 and Oracle).
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Partial, case-insensitive name search across first name, father's last name,
     * and mother's last name.
     *
     * <p>Supports the {@code GET /employees/search?name={name}} endpoint.
     * A search term of {@code "garcia"} will match employees whose first name,
     * father's last name, OR mother's last name contains the substring
     * {@code "garcia"} in any casing (e.g. "Garcia", "GARCIA", "garcia").
     *
     * <p>JPQL LOWER + CONCAT used instead of the Spring Data
     * {@code ContainingIgnoreCase} derived query to keep the logic explicit
     * and ensure identical behavior on Oracle and H2.
     *
     * @param name the partial name to search for (must not be {@code null})
     * @return a list of matching employees, or an empty list if none found
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE LOWER(e.firstName)      LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(e.fatherLastName) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(e.motherLastName) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    List<Employee> findByNameContainingIgnoreCase(@Param("name") String name);
}
