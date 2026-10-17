package com.artha.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Insurance coverage gap analysis result.
 *
 * SPEC REF: FR-6.2 — "Coverage gaps: (a) term life < 10× income → flag;
 * (b) health cover < ₹10L per family → flag."
 *
 * Each gap has a type, what's needed, what exists, and the shortfall.
 */
public record InsuranceCoverageGap(
        String gapType,           // "TERM_LIFE" or "HEALTH"
        String personName,        // Who the gap applies to
        BigDecimal recommended,   // What the spec says you need
        BigDecimal actual,        // What you actually have
        BigDecimal shortfall,     // recommended - actual
        String message            // Human-readable explanation
) {

    /** Convenience: build a list of all gaps for a household. */
    public static record CoverageAnalysis(
            List<InsuranceCoverageGap> gaps,
            boolean hasGaps
    ) {}
}
