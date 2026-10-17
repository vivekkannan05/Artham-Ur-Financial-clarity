package com.artha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating/updating an InsurancePolicy.
 * SPEC REF: FR-6.2
 */
public record InsurancePolicyRequest(
        @NotBlank(message = "Policy type is required")
        String policyType,

        @NotNull(message = "Insured person ID is required")
        UUID insuredPersonId,

        @NotNull(message = "Owner ID is required")
        UUID ownerId,

        String policyName,

        @NotBlank(message = "Insurer is required")
        String insurer,

        String policyNumber,

        @NotNull(message = "Sum assured is required")
        @Positive(message = "Sum assured must be positive")
        BigDecimal sumAssured,

        @NotNull(message = "Annual premium is required")
        @Positive(message = "Annual premium must be positive")
        BigDecimal annualPremium,

        LocalDate premiumDueDate,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        String renewalStatus,

        String notes
) {}
