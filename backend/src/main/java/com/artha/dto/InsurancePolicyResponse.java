package com.artha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO for InsurancePolicy. SPEC REF: FR-6.2 */
public record InsurancePolicyResponse(
        UUID id,
        UUID householdId,
        String policyType,
        UUID insuredPersonId,
        String insuredPersonName,
        UUID ownerId,
        String ownerName,
        String policyName,
        String insurer,
        String policyNumber,
        BigDecimal sumAssured,
        BigDecimal annualPremium,
        LocalDate premiumDueDate,
        LocalDate startDate,
        LocalDate endDate,
        String renewalStatus,
        String notes
) {}
