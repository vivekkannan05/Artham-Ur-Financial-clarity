package com.artha.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating/updating a Household.
 *
 * WHY A JAVA RECORD?
 *   Records are a Java 21 feature (introduced in Java 14, finalized in Java 16).
 *   A record automatically generates:
 *     - Constructor with all fields
 *     - Getter methods (name(), description() — no "get" prefix)
 *     - equals(), hashCode(), toString()
 *   Records are IMMUTABLE — fields can't be changed after creation.
 *   Perfect for DTOs (Data Transfer Objects) that carry request/response data.
 *
 * WHY NOT USE THE ENTITY DIRECTLY?
 *   1. Entities have JPA annotations and DB-specific fields (id, timestamps)
 *   2. Request body shouldn't include id or createdAt — they're server-generated
 *   3. Separating DTOs from entities prevents API changes from affecting DB schema
 *   4. You can validate the DTO independently (@NotBlank, @Size, etc.)
 */
public record HouseholdRequest(
        @NotBlank(message = "Household name is required")
        String name,

        String description
) {}
