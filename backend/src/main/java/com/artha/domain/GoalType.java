package com.artha.domain;

/**
 * Type of financial goal.
 *
 * SPEC REF: FR-4.1 — "type (retirement / child education / house purchase /
 * emergency fund / custom)"
 *
 * The goal type determines which glide-path strategy to use:
 *   RETIREMENT → age-based glide path (per FR-4.3)
 *   CHILD_EDUCATION, HOUSE → date-based glide path (per FR-4.3)
 *   EMERGENCY_FUND → no glide path (always liquid)
 *   CUSTOM → user chooses
 */
public enum GoalType {
    RETIREMENT,
    CHILD_EDUCATION,
    HOUSE,
    EMERGENCY_FUND,
    CUSTOM
}
