package com.artha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating/updating a Liability.
 *
 * SPEC REF: FR-5.1 — Liability data entry.
 *
 * WHY THESE FIELDS?
 *   - liabilityType: Determines how this debt is categorized (mortgage vs personal loan, etc.)
 *   - lender: Who you owe money to (bank name, NBFC, etc.)
 *   - principalOutstanding: Current balance — what you still owe
 *   - interestRate: Annual rate as percentage (e.g., 8.50)
 *   - emiAmount: Monthly payment amount — feeds into debt-service ratio
 *   - tenureStart/End: Loan period — used for payoff projections
 *   - prepaymentTotal: Any extra payments made beyond EMI
 *   - propertyId: Links to a Property entity for mortgages (Batch 5)
 */
public record LiabilityRequest(
        @NotBlank(message = "Liability type is required")
        String liabilityType,

        @NotBlank(message = "Lender is required")
        String lender,

        @NotNull(message = "Principal outstanding is required")
        @Positive(message = "Principal must be positive")
        BigDecimal principalOutstanding,

        BigDecimal originalPrincipal,

        @NotNull(message = "Interest rate is required")
        @PositiveOrZero(message = "Interest rate must be >= 0")
        BigDecimal interestRate,

        @NotNull(message = "EMI amount is required")
        @PositiveOrZero(message = "EMI must be >= 0")
        BigDecimal emiAmount,

        @NotNull(message = "Tenure start date is required")
        LocalDate tenureStart,

        LocalDate tenureEnd,

        @PositiveOrZero(message = "Prepayment must be >= 0")
        BigDecimal prepaymentTotal,

        java.util.UUID propertyId,

        String notes
) {}
