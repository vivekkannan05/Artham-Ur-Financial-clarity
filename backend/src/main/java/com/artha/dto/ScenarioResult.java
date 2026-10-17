package com.artha.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Scenario analysis result — the "what-if" output.
 *
 * SPEC REF: FR-6.4, EC-13 — Even extreme scenarios (negative net worth)
 * are displayed with a caveat, never suppressed.
 */
public record ScenarioResult(
        Map<String, BigDecimal> appliedOverrides,
        List<GoalScenarioImpact> goalImpacts,
        BigDecimal projectedNetWorth,
        BigDecimal projectedFreeCashFlow,
        BigDecimal netWorthDelta,        // vs current
        BigDecimal freeCashFlowDelta,    // vs current
        boolean isExtremeScenario,
        String caveat                    // EC-13: warning for extreme inputs
) {
    /**
     * Per-goal impact of the scenario.
     */
    public record GoalScenarioImpact(
            String goalName,
            BigDecimal originalFundingPct,
            BigDecimal scenarioFundingPct,
            BigDecimal fundingDelta,
            BigDecimal originalShortfall,
            BigDecimal scenarioShortfall
    ) {}
}
