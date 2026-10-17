package com.artha.domain;

/**
 * Emergency fund adequacy status.
 *
 * SPEC REF: FR-5.2 — "adequacy status (Adequate / Partial / Critical)."
 *
 * WHY THREE LEVELS (not just yes/no)?
 *   In real financial planning:
 *   - ADEQUATE: You have >= target months covered. You're safe.
 *   - PARTIAL: You have some reserve but less than target (e.g., 3-5 months
 *     when target is 6). Not an emergency, but needs attention.
 *   - CRITICAL: Less than half the target (e.g., 0-2 months). This is a
 *     red flag — the advisor should flag this as highest priority after goal adequacy.
 *
 * THRESHOLD LOGIC:
 *   monthsCovered >= target          → ADEQUATE
 *   monthsCovered >= target / 2      → PARTIAL
 *   monthsCovered < target / 2       → CRITICAL
 */
public enum EmergencyFundAdequacy {
    ADEQUATE,
    PARTIAL,
    CRITICAL
}
