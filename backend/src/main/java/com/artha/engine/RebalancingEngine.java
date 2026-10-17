package com.artha.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rebalancing Engine — Detects allocation drift and recommends corrections.
 *
 * SPEC REF: FR-4.4, Design Doc A-7
 *
 * When the actual equity/safe split deviates from the glide-path target
 * by more than the threshold (default 5%), this engine recommends:
 *   1. First: redirect new SIP contributions to the underweight class
 *   2. Only if that's insufficient: recommend selling from the overweight class
 */
public class RebalancingEngine {

    /** Default rebalancing threshold per SPEC-BINDING FR-4.4 = 5% */
    public static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("5.00");

    /**
     * Result of a rebalancing check.
     */
    public record RebalanceResult(
            boolean rebalancingNeeded,
            BigDecimal actualEquityPct,
            BigDecimal targetEquityPct,
            BigDecimal deviationPct,
            String recommendation
    ) {}

    /**
     * Check if rebalancing is needed and generate a recommendation.
     *
     * @param actualEquityPct Current equity % in the goal portfolio
     * @param targetEquityPct Target equity % from glide path
     * @param threshold Deviation threshold before triggering (default 5%)
     * @return Rebalancing result with recommendation
     */
    public static RebalanceResult check(BigDecimal actualEquityPct,
                                         BigDecimal targetEquityPct,
                                         BigDecimal threshold) {
        if (actualEquityPct == null || targetEquityPct == null) {
            return new RebalanceResult(false, actualEquityPct, targetEquityPct,
                    BigDecimal.ZERO, "Insufficient data for rebalancing check");
        }

        BigDecimal deviation = actualEquityPct.subtract(targetEquityPct).abs()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectiveThreshold = threshold != null ? threshold : DEFAULT_THRESHOLD;

        if (deviation.compareTo(effectiveThreshold) <= 0) {
            return new RebalanceResult(false, actualEquityPct, targetEquityPct,
                    deviation, "Within threshold — no rebalancing needed");
        }

        // Determine direction and recommendation
        String recommendation;
        if (actualEquityPct.compareTo(targetEquityPct) > 0) {
            // Over-allocated to equity → redirect SIPs to safe instruments
            recommendation = String.format(
                    "Equity over-allocated by %.2f%%. Redirect new SIP contributions to safe instruments (debt/FD). "
                    + "If contribution-based rebalancing is insufficient, consider partial equity redemption.",
                    deviation.doubleValue());
        } else {
            // Under-allocated to equity → redirect SIPs to equity instruments
            recommendation = String.format(
                    "Equity under-allocated by %.2f%%. Redirect new SIP contributions to equity instruments.",
                    deviation.doubleValue());
        }

        return new RebalanceResult(true, actualEquityPct, targetEquityPct,
                deviation, recommendation);
    }

    private RebalancingEngine() {}
}
