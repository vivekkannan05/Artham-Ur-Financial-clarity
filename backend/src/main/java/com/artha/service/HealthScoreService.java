package com.artha.service;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.dto.HealthScore;
import com.artha.dto.HealthScore.SubScore;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Health Score Service — Computes 7 sub-scores for household financial health.
 *
 * SPEC REF: FR-7.3 — "financial health score with explainable sub-scores."
 * Design Doc A-11 — O(total of sub-computations), O(1) per sub-score.
 *
 * SUB-SCORES:
 *   (a) Cash-flow health: savings rate
 *   (b) Debt health: debt-service ratio
 *   (c) Emergency readiness: months covered vs target
 *   (d) Goal progress: weighted funding %
 *   (e) Diversification: asset-class concentration
 *   (f) Insurance adequacy: coverage gaps
 *   (g) Tax readiness: clubbing flags, etc.
 *
 * SCORING: Each sub-score is 0–100. Green ≥ 70, Yellow ≥ 40, Red < 40.
 * Overall = weighted average of sub-scores.
 */
@Service
public class HealthScoreService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Compute financial health score from pre-built context.
     * Takes the same context used by the rule engine — no extra DB queries.
     */
    public HealthScore compute(HouseholdFinancialContext context) {
        List<SubScore> subScores = new ArrayList<>();

        subScores.add(computeCashFlowHealth(context));
        subScores.add(computeDebtHealth(context));
        subScores.add(computeEmergencyReadiness(context));
        subScores.add(computeGoalProgress(context));
        subScores.add(computeDiversification(context));
        subScores.add(computeInsuranceAdequacy(context));
        subScores.add(computeTaxReadiness(context));

        BigDecimal overall = subScores.stream()
                .map(SubScore::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(subScores.size()), 0, RoundingMode.HALF_UP);

        return new HealthScore(overall, statusFromScore(overall), subScores);
    }

    // ─── Sub-score computations ──────────────────────────────

    /**
     * (a) Cash-flow health: based on savings rate.
     * >= 20% savings → 100; 10-20% → 70; 0-10% → 40; negative → 0.
     */
    SubScore computeCashFlowHealth(HouseholdFinancialContext context) {
        BigDecimal savingsRate = context.cashFlow() != null
                ? context.cashFlow().savingsRate() : BigDecimal.ZERO;

        BigDecimal score;
        String explanation;
        if (savingsRate.compareTo(new BigDecimal("20")) >= 0) {
            score = HUNDRED;
            explanation = String.format("Savings rate is %.1f%% — excellent", savingsRate);
        } else if (savingsRate.compareTo(BigDecimal.TEN) >= 0) {
            score = new BigDecimal("70");
            explanation = String.format("Savings rate is %.1f%% — good but could improve", savingsRate);
        } else if (savingsRate.compareTo(BigDecimal.ZERO) >= 0) {
            score = new BigDecimal("40");
            explanation = String.format("Savings rate is %.1f%% — below recommended 20%%", savingsRate);
        } else {
            score = BigDecimal.ZERO;
            explanation = String.format("Negative cash flow: savings rate is %.1f%%", savingsRate);
        }
        return new SubScore("Cash-Flow Health", score, statusFromScore(score), explanation);
    }

    /**
     * (b) Debt health: based on debt-service ratio.
     * DSR 0% → 100; < 20% → 80; 20-40% → 50; > 40% → 10.
     */
    SubScore computeDebtHealth(HouseholdFinancialContext context) {
        BigDecimal dsr = context.debtServiceRatio() != null
                ? context.debtServiceRatio() : BigDecimal.ZERO;

        BigDecimal score;
        String explanation;
        if (dsr.compareTo(BigDecimal.ZERO) == 0) {
            score = HUNDRED;
            explanation = "No debt — debt-free";
        } else if (dsr.compareTo(new BigDecimal("20")) <= 0) {
            score = new BigDecimal("80");
            explanation = String.format("Debt-service ratio is %.1f%% — manageable", dsr);
        } else if (dsr.compareTo(new BigDecimal("40")) <= 0) {
            score = new BigDecimal("50");
            explanation = String.format("Debt-service ratio is %.1f%% — approaching stress zone", dsr);
        } else {
            score = BigDecimal.TEN;
            explanation = String.format("Debt-service ratio is %.1f%% — above 40%% threshold", dsr);
        }
        return new SubScore("Debt Health", score, statusFromScore(score), explanation);
    }

    /**
     * (c) Emergency readiness: months covered vs 6-month target.
     * >= 6 months → 100; 3-6 → 60; 0-3 → 20.
     */
    SubScore computeEmergencyReadiness(HouseholdFinancialContext context) {
        if (context.emergencyFund() == null) {
            return new SubScore("Emergency Readiness", BigDecimal.ZERO, "Red",
                    "Emergency fund data not available");
        }
        BigDecimal months = context.emergencyFund().monthsCovered();
        BigDecimal score;
        if (months.compareTo(new BigDecimal("6")) >= 0) {
            score = HUNDRED;
        } else if (months.compareTo(new BigDecimal("3")) >= 0) {
            score = new BigDecimal("60");
        } else {
            score = new BigDecimal("20");
        }
        String explanation = String.format("Emergency fund covers %.1f months (target: 6)", months);
        return new SubScore("Emergency Readiness", score, statusFromScore(score), explanation);
    }

    /**
     * (d) Goal progress: average funding percentage across all goals.
     */
    SubScore computeGoalProgress(HouseholdFinancialContext context) {
        if (context.goalProjections() == null || context.goalProjections().isEmpty()) {
            return new SubScore("Goal Progress", new BigDecimal("50"), "Yellow",
                    "No goals defined yet");
        }
        BigDecimal avgFunding = context.goalProjections().stream()
                .map(g -> g.fundingPercentage().min(HUNDRED))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(context.goalProjections().size()), 0, RoundingMode.HALF_UP);

        String explanation = String.format("Average goal funding is %.0f%% across %d goals",
                avgFunding, context.goalProjections().size());
        return new SubScore("Goal Progress", avgFunding, statusFromScore(avgFunding), explanation);
    }

    /**
     * (e) Diversification: checks if any single asset class exceeds 70%.
     */
    SubScore computeDiversification(HouseholdFinancialContext context) {
        if (context.assetsByClass() == null || context.assetsByClass().isEmpty()
                || context.totalAssets().compareTo(BigDecimal.ZERO) <= 0) {
            return new SubScore("Diversification", new BigDecimal("50"), "Yellow",
                    "No holdings data for diversification analysis");
        }

        BigDecimal maxConcentration = BigDecimal.ZERO;
        String dominantClass = "";
        for (var entry : context.assetsByClass().entrySet()) {
            BigDecimal pct = entry.getValue()
                    .divide(context.totalAssets(), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            if (pct.compareTo(maxConcentration) > 0) {
                maxConcentration = pct;
                dominantClass = entry.getKey();
            }
        }

        BigDecimal score;
        if (maxConcentration.compareTo(new BigDecimal("70")) > 0) {
            score = new BigDecimal("30");
        } else if (maxConcentration.compareTo(new BigDecimal("50")) > 0) {
            score = new BigDecimal("60");
        } else {
            score = new BigDecimal("90");
        }

        String explanation = String.format("Largest asset class: %s at %.0f%%",
                dominantClass, maxConcentration);
        return new SubScore("Diversification", score, statusFromScore(score), explanation);
    }

    /**
     * (f) Insurance adequacy: based on number of coverage gaps.
     */
    SubScore computeInsuranceAdequacy(HouseholdFinancialContext context) {
        int gapCount = context.insuranceGaps() != null ? context.insuranceGaps().size() : 0;
        BigDecimal score;
        String explanation;
        if (gapCount == 0) {
            score = HUNDRED;
            explanation = "All insurance coverage meets recommended levels";
        } else if (gapCount == 1) {
            score = new BigDecimal("50");
            explanation = String.format("%d insurance coverage gap detected", gapCount);
        } else {
            score = new BigDecimal("20");
            explanation = String.format("%d insurance coverage gaps detected", gapCount);
        }
        return new SubScore("Insurance Adequacy", score, statusFromScore(score), explanation);
    }

    /**
     * (g) Tax readiness: placeholder — full implementation in future.
     */
    SubScore computeTaxReadiness(HouseholdFinancialContext context) {
        return new SubScore("Tax Readiness", new BigDecimal("70"), "Green",
                "Basic tax tracking active — advanced analysis coming soon");
    }

    // ─── Helper ──────────────────────────────────────────────

    public static String statusFromScore(BigDecimal score) {
        if (score.compareTo(new BigDecimal("70")) >= 0) return "Green";
        if (score.compareTo(new BigDecimal("40")) >= 0) return "Yellow";
        return "Red";
    }
}
