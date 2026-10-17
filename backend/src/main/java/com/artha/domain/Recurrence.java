package com.artha.domain;

/** Recurrence pattern for calendar events. SPEC REF: FR-7.4 */
public enum Recurrence {
    NONE,       // One-time event
    MONTHLY,    // Every month (SIPs, EMIs)
    QUARTERLY,  // Every 3 months
    ANNUAL      // Once a year (renewals, reviews)
}
