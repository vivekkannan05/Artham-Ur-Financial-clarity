package com.artha.domain;

/** Type of financial calendar event. SPEC REF: FR-7.4 */
public enum CalendarEventType {
    SIP,            // SIP installment date
    EMI,            // EMI due date
    RENEWAL,        // Insurance renewal
    MATURITY,       // FD/RD maturity
    TAX_DEADLINE,   // Tax filing deadline
    GOAL_REVIEW,    // Annual goal review
    CUSTOM          // User-defined reminder
}
