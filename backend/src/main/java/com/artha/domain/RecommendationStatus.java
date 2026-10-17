package com.artha.domain;

/**
 * Status of an advisor recommendation (user decision lifecycle).
 * SPEC REF: FR-7.2 — "status (pending / accepted / rejected / deferred)."
 */
public enum RecommendationStatus {
    PENDING,    // Awaiting user decision
    ACCEPTED,   // User agreed and will act
    REJECTED,   // User chose not to act
    DEFERRED    // User will decide later
}
