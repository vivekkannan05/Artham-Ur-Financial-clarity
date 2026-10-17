package com.artha.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Scenario analysis request — what-if modeling.
 *
 * SPEC REF: FR-6.4 — "User can modify one or more variables and immediately
 * see the impact on goal funding, corpus projections, cash flow, and net worth.
 * Scenario results are computed on-the-fly and do NOT modify stored data."
 *
 * HOW IT WORKS:
 *   The user sends a map of variable overrides (e.g., {"inflationRate": 8, "sipAmount": 15000}).
 *   The engine re-runs projections with these overrides instead of actual values.
 *   The response shows the delta: what changes if these hypothetical values were true.
 */
public record ScenarioRequest(
        /** Variable overrides. Keys: inflationRate, expectedReturn, sipAmount,
         *  sipStepUpPct, retirementAge, marketCrashPct, jobLossMonths, interestRateChange */
        Map<String, BigDecimal> overrides
) {}
