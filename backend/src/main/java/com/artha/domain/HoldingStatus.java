package com.artha.domain;

/**
 * Status of a holding (investment position).
 *
 * SPEC REF: FR-3.3 — "status (active/closed/matured)"
 * EC-10: "Full redemption — mark holding as closed. Retain for historical XIRR."
 */
public enum HoldingStatus {
    ACTIVE,     // Currently held, has units > 0
    CLOSED,     // Fully redeemed (zero units) — retained for historical analytics
    MATURED     // Fixed-term instrument reached maturity (FD, RD, SGB, PPF, etc.)
}
