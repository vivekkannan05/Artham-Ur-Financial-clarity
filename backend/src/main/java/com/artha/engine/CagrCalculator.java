package com.artha.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * CAGR Calculator — Compound Annual Growth Rate.
 *
 * WHAT IS CAGR?
 *   CAGR is the smoothed annual growth rate between two values over a period.
 *   Unlike XIRR, it doesn't consider intermediate cash flows — just start and end.
 *
 * FORMULA:
 *   CAGR = (endValue / startValue) ^ (1 / years) - 1
 *
 * EXAMPLE:
 *   Invested ₹1,00,000 → grew to ₹1,61,051 in 5 years
 *   CAGR = (161051 / 100000) ^ (1/5) - 1 = 0.10 = 10% per year
 *
 * WHEN TO USE CAGR vs XIRR:
 *   CAGR: Simple start→end growth (e.g., "How did this FD perform?")
 *   XIRR: Multiple cash flows at different times (e.g., "How did my SIP perform?")
 *
 * SPEC REF: FR-3.6, Design Doc A-2
 */
public class CagrCalculator {

    private static final int RESULT_SCALE = 6;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    /**
     * Calculate CAGR.
     *
     * @param startValue Initial investment value (must be > 0)
     * @param endValue   Final value (must be > 0)
     * @param years      Number of years (must be > 0)
     * @return CAGR as decimal (e.g., 0.10 = 10%), or empty if inputs are invalid
     */
    public static Optional<BigDecimal> calculate(BigDecimal startValue,
                                                  BigDecimal endValue,
                                                  BigDecimal years) {
        // Validate inputs
        if (startValue == null || endValue == null || years == null) {
            return Optional.empty();
        }
        if (startValue.signum() <= 0 || endValue.signum() <= 0 || years.signum() <= 0) {
            return Optional.empty();
        }

        // CAGR = (endValue / startValue) ^ (1 / years) - 1
        // Using double for the power operation, then converting back to BigDecimal
        double ratio = endValue.doubleValue() / startValue.doubleValue();
        double exponent = 1.0 / years.doubleValue();
        double cagr = Math.pow(ratio, exponent) - 1.0;

        if (Double.isNaN(cagr) || Double.isInfinite(cagr)) {
            return Optional.empty();
        }

        return Optional.of(BigDecimal.valueOf(cagr)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP));
    }

    private CagrCalculator() {} // Utility class
}
