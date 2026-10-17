package com.artha.engine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single cash flow entry for XIRR calculation.
 *
 * SIGN CONVENTION:
 *   Negative amount = money going OUT (investment/purchase)
 *   Positive amount = money coming IN (redemption/dividend/current value)
 */
public record CashFlow(
        LocalDate date,
        BigDecimal amount
) {
    /**
     * Factory for an investment (outflow).
     * Amount is stored as negative.
     */
    public static CashFlow investment(LocalDate date, BigDecimal amount) {
        return new CashFlow(date, amount.negate());
    }

    /**
     * Factory for a return/redemption (inflow).
     * Amount is stored as positive.
     */
    public static CashFlow redemption(LocalDate date, BigDecimal amount) {
        return new CashFlow(date, amount);
    }
}
