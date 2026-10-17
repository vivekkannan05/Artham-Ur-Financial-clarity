package com.artha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Expense.
 */
public record ExpenseResponse(
        UUID id,
        UUID personId,
        String name,
        BigDecimal amount,
        String frequency,
        String category,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {}
