package com.artha.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Person.
 *
 * SECURITY: panMasked shows masked PAN (e.g., "ABCD****4F"), NEVER the full PAN.
 */
public record PersonResponse(
        UUID id,
        String name,
        String relationship,
        LocalDate dateOfBirth,
        String gender,
        String panMasked,           // Masked PAN — never expose the full PAN
        String taxRegime,
        String residencyStatus,
        String citizenship,
        UUID householdId,
        Instant createdAt,
        Instant updatedAt
) {}
