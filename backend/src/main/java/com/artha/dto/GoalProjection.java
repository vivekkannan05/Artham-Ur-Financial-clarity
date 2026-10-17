package com.artha.dto;

import java.math.BigDecimal;

/**
 * Goal funding projection — how close a goal is to being funded.
 * SPEC REF: FR-4.2
 */
public record GoalProjection(
        BigDecimal targetAmountToday,
        BigDecimal inflationAdjustedTarget,
        int yearsToGoal,
        BigDecimal currentCorpus,
        BigDecimal projectedCorpus,
        BigDecimal fundingPercentage,
        BigDecimal shortfallOrSurplus,
        BigDecimal requiredMonthlySip,
        BigDecimal requiredSipWithStepUp
) {}
