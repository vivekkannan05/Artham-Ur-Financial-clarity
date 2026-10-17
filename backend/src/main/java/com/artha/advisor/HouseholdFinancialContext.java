package com.artha.advisor;

import com.artha.domain.EmergencyFundAdequacy;
import com.artha.dto.CashFlowStatement;
import com.artha.dto.EmergencyFundStatus;
import com.artha.dto.GoalProjection;
import com.artha.dto.InsuranceCoverageGap;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HouseholdFinancialContext — The shared context for all rule evaluations.
 *
 * SPEC REF: FR-7.1 — "Layer 1 deterministic calculations produce numeric facts."
 * Design Doc Sec 9.3 — "Built ONCE and shared across all rules and the AI explainer."
 *
 * WHY A SINGLE CONTEXT?
 *   Without this, each rule would independently query the database for the same
 *   data — N rules × M queries = waste. Instead, we load everything once into
 *   this context object, then pass it to every rule. O(1) DB round-trip.
 */
public record HouseholdFinancialContext(
        UUID householdId,
        String householdName,
        int memberCount,

        // Cash Flow (from CashFlowService)
        CashFlowStatement cashFlow,

        // Goals (from GoalService projections)
        List<GoalProjection> goalProjections,
        List<String> goalNames,

        // Emergency Fund (from EmergencyFundCalculator)
        EmergencyFundStatus emergencyFund,

        // Net Worth
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        Map<String, BigDecimal> assetsByClass,

        // Insurance gaps (from InsuranceService)
        List<InsuranceCoverageGap> insuranceGaps,

        // Debt
        BigDecimal debtServiceRatio
) {}
