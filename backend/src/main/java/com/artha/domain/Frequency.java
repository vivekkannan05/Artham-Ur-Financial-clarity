package com.artha.domain;

/**
 * How often a recurring income or expense repeats.
 *
 * SPEC REF: FR-2.1 (income frequency), FR-2.2 (expense frequency)
 */
public enum Frequency {
    ONE_TIME,     // Single occurrence, no recurrence
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL
}
