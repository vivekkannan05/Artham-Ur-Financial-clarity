package com.artha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request DTO for Property. SPEC REF: FR-6.3 */
public record PropertyRequest(
        @NotBlank(message = "Property name is required")
        String name,

        String address,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,

        @NotNull(message = "Current value is required")
        @Positive(message = "Current value must be positive")
        BigDecimal currentValue,

        LocalDate valuationDate,
        BigDecimal monthlyRentalIncome,
        BigDecimal monthlyExpenses,
        Boolean selfOccupied,
        String notes
) {}
