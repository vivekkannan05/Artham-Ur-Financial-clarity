package com.artha.domain;

/**
 * Type of income source.
 *
 * SPEC REF: FR-2.1 — "Income types include: salary, rental, interest, dividends,
 * bonuses, business income, capital gains, and other."
 */
public enum IncomeType {
    SALARY,
    RENTAL,
    INTEREST,
    DIVIDENDS,
    BONUS,
    BUSINESS,
    CAPITAL_GAINS,
    OTHER
}
