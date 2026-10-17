package com.artha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Income.
 */
public record IncomeResponse(
        UUID id,
        UUID personId,
        String incomeType,
        BigDecimal amount,
        String frequency,
        LocalDate startDate,
        LocalDate endDate,
        String source,
        Instant createdAt,
        Instant updatedAt
) {}
