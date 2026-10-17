package com.artha.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Glide-Path Engine — Computes target asset allocation at a point in time.
 *
 * SPEC REF: FR-4.3, Design Doc A-6 (Glide-Path Interpolation)
 *
 * A glide path gradually shifts a portfolio from aggressive (equity-heavy)
 * to conservative (debt-heavy) as the goal date approaches.
 *
 * WHY?
 *   When your goal is 15 years away, you can afford stock market volatility.
 *   When it's 1 year away, you can't — so you shift to safe instruments.
 */
public class GlidePathEngine {

    /**
     * A single milestone in the glide-path schedule.
     */
    public record Milestone(double yearsToGoal, BigDecimal equityPct, BigDecimal safePct) {}

    /**
     * The computed target allocation at a point in time.
     */
    public record Allocation(BigDecimal equityPct, BigDecimal safePct) {}

    /**
     * Interpolate the target allocation for a given years-to-goal value.
     *
     * Uses linear interpolation between the two nearest milestones.
     * If past the target date (years < 0), returns terminal (most conservative) allocation.
     *
     * @param milestones Sorted by yearsToGoal DESCENDING (farthest first)
     * @param currentYearsToGoal Can be negative if goal date has passed (EC-9)
     * @return Target allocation
     */
    public static Allocation interpolate(Milestone[] milestones, double currentYearsToGoal) {
        if (milestones == null || milestones.length == 0) {
            return new Allocation(new BigDecimal("50.00"), new BigDecimal("50.00"));
        }

        // Before first milestone (far from goal) — use the farthest allocation
        if (currentYearsToGoal >= milestones[0].yearsToGoal) {
            return new Allocation(milestones[0].equityPct, milestones[0].safePct);
        }

        // Past last milestone or goal overdue (EC-9) — use terminal allocation
        Milestone last = milestones[milestones.length - 1];
        if (currentYearsToGoal <= last.yearsToGoal) {
            return new Allocation(last.equityPct, last.safePct);
        }

        // Find the two bracketing milestones and interpolate
        for (int i = 0; i < milestones.length - 1; i++) {
            Milestone upper = milestones[i];
            Milestone lower = milestones[i + 1];

            if (currentYearsToGoal <= upper.yearsToGoal
                    && currentYearsToGoal > lower.yearsToGoal) {

                double range = upper.yearsToGoal - lower.yearsToGoal;
                double fraction = (upper.yearsToGoal - currentYearsToGoal) / range;

                BigDecimal equity = upper.equityPct.subtract(
                        BigDecimal.valueOf(fraction).multiply(
                                upper.equityPct.subtract(lower.equityPct)
                        )
                ).setScale(2, RoundingMode.HALF_UP);

                BigDecimal safe = BigDecimal.valueOf(100).subtract(equity)
                        .setScale(2, RoundingMode.HALF_UP);

                return new Allocation(equity, safe);
            }
        }

        // Fallback (shouldn't reach here with valid schedule)
        return new Allocation(last.equityPct, last.safePct);
    }

    private GlidePathEngine() {}
}
