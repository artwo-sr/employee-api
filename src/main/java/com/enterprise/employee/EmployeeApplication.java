package com.enterprise.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Employee Microservice.
 *
 * <p>{@link SpringBootApplication} enables:
 * <ul>
 *   <li>Component scanning from this package downward</li>
 *   <li>Spring Boot auto-configuration</li>
 *   <li>Configuration property binding</li>
 * </ul>
 */
@SpringBootApplication
public class EmployeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeApplication.class, args);
    }
}
