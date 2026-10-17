package com.artha.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * XIRR Calculator — Extended Internal Rate of Return.
 *
 * WHAT IS XIRR?
 *   XIRR is the annualized return on a series of cash flows that happen at
 *   irregular dates. Unlike CAGR (which only needs start/end), XIRR handles
 *   multiple investments and redemptions at different times.
 *
 *   It finds the rate 'r' that makes the Net Present Value (NPV) = 0:
 *     NPV = Σ (amount_i / (1 + r) ^ years_i) = 0
 *
 * ALGORITHM: Newton-Raphson iterative root-finding.
 *   1. Start with an initial guess (10%)
 *   2. Compute NPV and its derivative at the current guess
 *   3. Adjust guess: rate = rate - NPV/derivative
 *   4. Repeat until NPV ≈ 0 or max iterations reached
 *
 * WHY NEWTON-RAPHSON?
 *   - Converges fast (typically 10-15 iterations)
 *   - Industry standard for XIRR (Excel uses the same approach)
 *   - O(n × k) time where n=cash flows, k=iterations
 *
 * SPEC REF: FR-3.6, Design Doc A-1
 *
 * This class is a stateless utility — no Spring annotations needed.
 * It could be a @Component but there's no benefit for a pure function.
 * (per AD-7: no unnecessary interfaces for calculators)
 */
public class XirrCalculator {

    /** Maximum iterations before giving up (per Design Doc pseudocode) */
    private static final int MAX_ITERATIONS = 100;

    /** Convergence tolerance — stop when |NPV| < this value */
    private static final BigDecimal TOLERANCE = new BigDecimal("1E-7");

    /** Days in a year for the year-fraction calculation */
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365.0");

    /** Precision context for intermediate BigDecimal math */
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    /** Scale for the final result (per NFR-1.2: RATE_SCALE = 6) */
    private static final int RESULT_SCALE = 6;

    /**
     * Calculate XIRR for a list of cash flows.
     *
     * @param cashFlows List of (date, amount) pairs. Negative = outflow, positive = inflow.
     *                  Must contain at least one positive and one negative cash flow.
     * @return The annualized rate as a decimal (e.g., 0.12 means 12%), or empty if
     *         calculation is not possible (insufficient data, no convergence).
     */
    public static Optional<BigDecimal> calculate(List<CashFlow> cashFlows) {
        // Edge case EC-1: insufficient data
        if (cashFlows == null || cashFlows.size() < 2) {
            return Optional.empty();
        }

        // Sort by date ascending
        List<CashFlow> sorted = cashFlows.stream()
                .sorted(Comparator.comparing(CashFlow::date))
                .toList();

        LocalDate firstDate = sorted.getFirst().date();

        // Verify we have both inflows and outflows
        boolean hasNegative = sorted.stream().anyMatch(cf -> cf.amount().signum() < 0);
        boolean hasPositive = sorted.stream().anyMatch(cf -> cf.amount().signum() > 0);
        if (!hasNegative || !hasPositive) {
            return Optional.empty();
        }

        // Newton-Raphson iteration
        double rate = 0.10; // Initial guess: 10%

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double npv = 0.0;
            double dnpv = 0.0; // derivative of NPV with respect to rate

            for (CashFlow cf : sorted) {
                double years = ChronoUnit.DAYS.between(firstDate, cf.date()) / 365.0;
                double denominator = Math.pow(1.0 + rate, years);

                if (denominator == 0.0) {
                    return Optional.empty(); // Avoid division by zero
                }

                npv += cf.amount().doubleValue() / denominator;
                dnpv += -years * cf.amount().doubleValue() / denominator / (1.0 + rate);
            }

            // Check convergence
            if (Math.abs(npv) < TOLERANCE.doubleValue()) {
                return Optional.of(BigDecimal.valueOf(rate)
                        .setScale(RESULT_SCALE, RoundingMode.HALF_UP));
            }

            // Avoid division by zero in Newton step
            if (Math.abs(dnpv) < 1e-15) {
                return Optional.empty();
            }

            // Newton-Raphson step: rate = rate - f(rate)/f'(rate)
            rate = rate - npv / dnpv;

            // Safety: rate should stay in reasonable bounds
            if (rate < -0.999 || rate > 100.0) {
                return Optional.empty();
            }
        }

        // Did not converge within MAX_ITERATIONS
        return Optional.empty();
    }

    private XirrCalculator() {} // Utility class — no instantiation
}
