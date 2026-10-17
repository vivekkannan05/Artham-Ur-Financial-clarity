package com.artha.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID ownerId,
        String institutionName,
        String accountNumberMasked,
        String accountType,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
