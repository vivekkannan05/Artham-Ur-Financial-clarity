package com.artha.domain;

/**
 * Renewal status of an insurance policy.
 *
 * SPEC REF: FR-6.2 — "renewal status."
 *
 * WHY TRACK THIS?
 *   A lapsed policy provides ZERO coverage. If the user thinks they're covered
 *   but their policy lapsed, the advisor needs to flag this immediately.
 *   - ACTIVE: Policy is in force, premiums paid up to date.
 *   - LAPSED: Premium not paid — coverage has stopped.
 *   - EXPIRED: Policy term ended naturally.
 *   - PENDING_RENEWAL: Renewal due soon (within 30 days).
 */
public enum RenewalStatus {
    ACTIVE,
    LAPSED,
    EXPIRED,
    PENDING_RENEWAL
}
