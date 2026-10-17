package com.artha.domain;

/**
 * Type of correction to an immutable transaction.
 *
 * SPEC REF: FR-3.4 — "A separate transaction_correction table records adjustments."
 */
public enum CorrectionType {
    REVERSAL,       // Completely undo the original transaction
    ADJUSTMENT      // Partial correction (e.g., wrong amount)
}
