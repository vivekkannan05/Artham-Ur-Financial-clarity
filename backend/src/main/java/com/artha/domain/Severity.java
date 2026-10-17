package com.artha.domain;

/**
 * Severity level for advisor recommendations and rule results.
 * SPEC REF: FR-7.1 — "flags/recommendations with severity (Critical / Warning / Info)."
 */
public enum Severity {
    CRITICAL,   // Requires immediate attention (e.g., negative cash flow, no emergency fund)
    WARNING,    // Should be addressed soon (e.g., under-insured, allocation drift)
    INFO        // Nice to know (e.g., tax optimization opportunity)
}
