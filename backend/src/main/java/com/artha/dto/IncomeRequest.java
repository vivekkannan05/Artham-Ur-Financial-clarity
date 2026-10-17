package com.artha.dto;

import com.artha.domain.Frequency;
import com.artha.domain.IncomeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating/updating an Income entry.
 */
public record IncomeRequest(
        @NotNull(message = "Income type is required")
        IncomeType incomeType,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Frequency is required")
        Frequency frequency,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        String source
) {}
