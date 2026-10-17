package com.artha.domain;

/**
 * Status of a liability (debt lifecycle).
 *
 * SPEC REF: FR-5.1 — Liabilities have a lifecycle from active to closed/defaulted.
 *
 * WHY TRACK STATUS?
 *   - ACTIVE: EMI is being paid, this liability affects debt-service ratio and net worth.
 *   - CLOSED: Fully repaid — kept for historical record but excluded from current calculations.
 *   - DEFAULTED: Payments stopped — flagged as critical in financial health score.
 */
public enum LiabilityStatus {
    ACTIVE,
    CLOSED,
    DEFAULTED
}
