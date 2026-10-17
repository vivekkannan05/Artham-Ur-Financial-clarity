package com.artha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Artha — Household Financial Operating System
 *
 * This is the main entry point for the Spring Boot application.
 *
 * @SpringBootApplication combines three annotations:
 *   - @Configuration: Marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration: Tells Spring Boot to auto-configure based on dependencies
 *   - @ComponentScan: Scans com.artha.* packages for @Component, @Service, @Repository, @Controller
 *
 * To run: ./mvnw spring-boot:run (from the backend/ directory)
 */
@SpringBootApplication
public class ArthaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthaApplication.class, args);
    }
}
