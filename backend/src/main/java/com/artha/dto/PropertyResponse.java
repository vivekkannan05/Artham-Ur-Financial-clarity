package com.artha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Property with computed fields.
 *
 * SPEC REF: FR-6.3 — includes computed rentalYield and netEquity.
 *
 * FORMULA:
 *   rentalYield = (monthlyRentalIncome × 12 / currentValue) × 100
 *   netEquity = currentValue - mortgageOutstanding (from linked Liability)
 */
public record PropertyResponse(
        UUID id,
        UUID householdId,
        String name,
        String address,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        BigDecimal currentValue,
        LocalDate valuationDate,
        BigDecimal monthlyRentalIncome,
        BigDecimal monthlyExpenses,
        boolean selfOccupied,
        BigDecimal rentalYield,      // computed
        BigDecimal netEquity,        // computed (currentValue - mortgage)
        BigDecimal annualNetIncome,  // computed (rent - expenses) × 12
        String notes
) {}
