package com.artha.domain;

/**
 * Whether an expense is consumption spending or an investment outflow.
 *
 * SPEC REF: FR-2.2 — "a flag distinguishing consumption (groceries, utilities)
 * from investment outflow (SIP, EMI principal)."
 *
 * WHY THIS MATTERS:
 *   Cash-flow calculation treats these differently:
 *   - CONSUMPTION: Reduces free cash flow. It's money spent.
 *   - INVESTMENT_OUTFLOW: Also reduces free cash flow, but it's money invested
 *     (SIPs, EMI principal). The savings rate calculation may treat these differently.
 */
public enum ExpenseCategory {
    CONSUMPTION,         // Groceries, utilities, rent, entertainment
    INVESTMENT_OUTFLOW   // SIP payments, EMI principal portion
}
