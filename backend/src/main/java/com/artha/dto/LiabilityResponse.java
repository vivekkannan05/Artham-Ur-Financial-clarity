package com.artha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Liability.
 *
 * SPEC REF: FR-5.1 — Liability display data.
 *
 * WHY A SEPARATE RESPONSE DTO (not returning the entity directly)?
 *   1. Decouples API contract from database schema — we can evolve either independently.
 *   2. Avoids serializing JPA proxies (Household lazy-load would trigger N+1 queries).
 *   3. Allows adding computed fields (like remainingTenureMonths) without a DB column.
 */
public record LiabilityResponse(
        UUID id,
        UUID householdId,
        String liabilityType,
        String status,
        String lender,
        BigDecimal principalOutstanding,
        BigDecimal originalPrincipal,
        BigDecimal interestRate,
        BigDecimal emiAmount,
        LocalDate tenureStart,
        LocalDate tenureEnd,
        BigDecimal prepaymentTotal,
        UUID propertyId,
        String notes
) {}
