package com.artha.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Household.
 * Returned by the API — includes server-generated fields (id, timestamps).
 */
public record HouseholdResponse(
        UUID id,
        String name,
        String description,
        String status,
        List<PersonResponse> members,
        Instant createdAt,
        Instant updatedAt
) {}
