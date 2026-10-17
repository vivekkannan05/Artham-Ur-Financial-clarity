package com.artha.engine;

import com.artha.domain.EmergencyFundAdequacy;
import com.artha.dto.EmergencyFundStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Emergency Fund Calculator — Evaluates household emergency preparedness.
 *
 * SPEC REF: FR-5.2 — "Calculate emergency fund adequacy."
 * SPEC-BINDING: FR-5.2 = EMERGENCY_FUND_MONTHS=6
 *
 * ALGORITHM (Design Doc A-10):
 *   1. requiredReserve = targetMonths × monthlyEssentialExpenses
 *   2. monthsCovered = currentLiquidReserve / monthlyEssentialExpenses
 *   3. shortfall = max(0, requiredReserve - currentLiquidReserve)
 *   4. adequacy = categorize(monthsCovered, targetMonths)
 *
 * Time complexity: O(1) — this engine receives pre-computed inputs.
 * The O(h) cost mentioned in the Design Doc is in the service layer that
 * sums holdings to compute currentLiquidReserve before calling this engine.
 *
 * WHY PURE STATIC METHODS?
 *   Same philosophy as XirrCalculator and CagrCalculator — pure functions
 *   with no state. Easy to test, easy to reason about. The service layer
 *   gathers the inputs; this engine just does the math.
 *
 * ADEQUACY THRESHOLDS:
 *   - ADEQUATE: monthsCovered >= targetMonths (you're fully covered)
 *   - PARTIAL: monthsCovered >= targetMonths / 2 (some coverage, needs attention)
 *   - CRITICAL: monthsCovered < targetMonths / 2 (urgent — advisor flags this)
 */
public final class EmergencyFundCalculator {

    private EmergencyFundCalculator() {
        // Static utility — no instantiation
    }

    /** Default target: 6 months of essential expenses (SPEC-BINDING FR-5.2). */
    public static final int DEFAULT_TARGET_MONTHS = 6;

    /**
     * Compute emergency fund status.
     *
     * @param monthlyEssentialExpenses Monthly consumption expenses (not SIPs/EMIs).
     *                                  This is the CONSUMPTION category sum from CashFlowService.
     * @param currentLiquidReserve     Sum of liquid holdings: savings accounts + liquid MFs
     *                                  + FDs maturing within 90 days.
     * @param targetMonths             How many months of expenses to keep as reserve
     *                                  (default: 6 per SPEC-BINDING FR-5.2).
     * @return EmergencyFundStatus with all computed values.
     */
    public static EmergencyFundStatus compute(BigDecimal monthlyEssentialExpenses,
                                               BigDecimal currentLiquidReserve,
                                               int targetMonths) {

        // ─── Guard: if no expenses, everything is "adequate" ───
        // A household with zero expenses needs zero emergency fund.
        if (monthlyEssentialExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return new EmergencyFundStatus(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    currentLiquidReserve,
                    new BigDecimal("999.00"),  // effectively infinite months
                    BigDecimal.ZERO,
                    EmergencyFundAdequacy.ADEQUATE
            );
        }

        // ─── Step 1: Required reserve ───
        BigDecimal requiredReserve = monthlyEssentialExpenses
                .multiply(BigDecimal.valueOf(targetMonths))
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Step 2: Months covered ───
        // How many months can the household survive on liquid reserves alone?
        BigDecimal monthsCovered = currentLiquidReserve
                .divide(monthlyEssentialExpenses, 2, RoundingMode.HALF_UP);

        // ─── Step 3: Shortfall ───
        BigDecimal shortfall = requiredReserve.subtract(currentLiquidReserve)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Step 4: Categorize adequacy ───
        EmergencyFundAdequacy adequacy = categorize(monthsCovered, targetMonths);

        return new EmergencyFundStatus(
                monthlyEssentialExpenses,
                requiredReserve,
                currentLiquidReserve,
                monthsCovered,
                shortfall,
                adequacy
        );
    }

    /**
     * Categorize emergency fund adequacy based on months covered vs target.
     *
     * The thresholds:
     *   >= target       → ADEQUATE  (e.g., 8 months covered, target 6)
     *   >= target / 2   → PARTIAL   (e.g., 4 months covered, target 6)
     *   < target / 2    → CRITICAL  (e.g., 1 month covered, target 6)
     */
    static EmergencyFundAdequacy categorize(BigDecimal monthsCovered, int targetMonths) {
        BigDecimal target = BigDecimal.valueOf(targetMonths);
        BigDecimal halfTarget = target.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        if (monthsCovered.compareTo(target) >= 0) {
            return EmergencyFundAdequacy.ADEQUATE;
        } else if (monthsCovered.compareTo(halfTarget) >= 0) {
            return EmergencyFundAdequacy.PARTIAL;
        } else {
            return EmergencyFundAdequacy.CRITICAL;
        }
    }
}
