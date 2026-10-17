package com.artha.dto;

import com.artha.domain.ExpenseCategory;
import com.artha.domain.Frequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating/updating an Expense entry.
 */
public record ExpenseRequest(
        @NotBlank(message = "Expense name is required")
        String name,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Frequency is required")
        Frequency frequency,

        @NotNull(message = "Category is required (CONSUMPTION or INVESTMENT_OUTFLOW)")
        ExpenseCategory category,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate
) {}
