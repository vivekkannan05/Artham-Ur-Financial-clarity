package com.artha.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Goal Funding Projection Engine — Computes funding status and required SIPs.
 *
 * SPEC REF: FR-4.2 — Goal funding projections.
 * DESIGN DOC: A-3 (SIP Future Value), A-4 (SIP with Step-Up), A-5 (Inflation Target)
 *
 * This engine answers the core question for every goal:
 *   "Am I on track? If not, how much more do I need to invest?"
 */
public class FundingProjectionEngine {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int CURRENCY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    /**
     * Compute the inflation-adjusted future target amount.
     * Formula: FV = PV × (1 + inflationRate) ^ years
     *
     * Example: ₹25,00,000 today at 6% inflation for 10 years
     *          = 25,00,000 × 1.06^10 = ₹44,77,120
     */
    public static BigDecimal inflationAdjustedTarget(BigDecimal presentValue,
                                                      BigDecimal annualInflation,
                                                      int years) {
        if (presentValue == null || annualInflation == null || years <= 0) {
            return presentValue != null ? presentValue : BigDecimal.ZERO;
        }
        double multiplier = Math.pow(1.0 + annualInflation.doubleValue(), years);
        return presentValue.multiply(BigDecimal.valueOf(multiplier), MC)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Project future value of a monthly SIP (no step-up).
     * Formula: FV = PMT × [((1+r)^n - 1) / r] × (1+r)
     *
     * SPEC REF: Design Doc A-3
     */
    public static BigDecimal sipFutureValue(BigDecimal monthlySip,
                                             BigDecimal annualReturn,
                                             int years) {
        if (monthlySip == null || annualReturn == null || years <= 0) {
            return BigDecimal.ZERO;
        }
        double r = annualReturn.doubleValue() / 12.0;
        int n = years * 12;

        if (Math.abs(r) < 1e-10) {
            return monthlySip.multiply(BigDecimal.valueOf(n))
                    .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        }

        double fv = monthlySip.doubleValue() * ((Math.pow(1 + r, n) - 1) / r) * (1 + r);
        return BigDecimal.valueOf(fv).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Project future value of a SIP with annual step-up.
     *
     * Each year the SIP amount increases by stepUpPct.
     * Example: ₹10,000/month with 10% step-up → ₹11,000 in year 2, ₹12,100 in year 3.
     *
     * SPEC REF: Design Doc A-4
     */
    public static BigDecimal sipFutureValueWithStepUp(BigDecimal monthlySip,
                                                       BigDecimal annualReturn,
                                                       int years,
                                                       BigDecimal stepUpPct) {
        if (monthlySip == null || annualReturn == null || years <= 0) {
            return BigDecimal.ZERO;
        }
        if (stepUpPct == null || stepUpPct.signum() == 0) {
            return sipFutureValue(monthlySip, annualReturn, years);
        }

        double monthlyRate = annualReturn.doubleValue() / 12.0;
        double totalValue = 0.0;
        double currentSip = monthlySip.doubleValue();

        for (int year = 1; year <= years; year++) {
            int monthsRemaining = (years - year) * 12 + 12;
            for (int month = 1; month <= 12; month++) {
                totalValue += currentSip * Math.pow(1 + monthlyRate, monthsRemaining);
                monthsRemaining--;
            }
            currentSip *= (1.0 + stepUpPct.doubleValue());
        }

        return BigDecimal.valueOf(totalValue).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Compute future value of existing corpus growing at expected return.
     * Formula: FV = PV × (1 + annualReturn) ^ years
     */
    public static BigDecimal corpusFutureValue(BigDecimal currentCorpus,
                                                BigDecimal annualReturn,
                                                int years) {
        if (currentCorpus == null || currentCorpus.signum() <= 0 || years <= 0) {
            return currentCorpus != null ? currentCorpus : BigDecimal.ZERO;
        }
        double fv = currentCorpus.doubleValue() * Math.pow(1 + annualReturn.doubleValue(), years);
        return BigDecimal.valueOf(fv).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Compute required monthly SIP to reach a shortfall amount.
     * Inverse of sipFutureValue: PMT = FV × r / [((1+r)^n - 1) × (1+r)]
     */
    public static BigDecimal requiredMonthlySip(BigDecimal shortfall,
                                                 BigDecimal annualReturn,
                                                 int years) {
        if (shortfall == null || shortfall.signum() <= 0 || years <= 0) {
            return BigDecimal.ZERO;
        }
        double r = annualReturn.doubleValue() / 12.0;
        int n = years * 12;

        if (Math.abs(r) < 1e-10) {
            return shortfall.divide(BigDecimal.valueOf(n), CURRENCY_SCALE, RoundingMode.HALF_UP);
        }

        double pmt = shortfall.doubleValue() * r / ((Math.pow(1 + r, n) - 1) * (1 + r));
        return BigDecimal.valueOf(pmt).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private FundingProjectionEngine() {}
}
