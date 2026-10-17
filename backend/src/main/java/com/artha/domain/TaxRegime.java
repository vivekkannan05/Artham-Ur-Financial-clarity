package com.artha.domain;

/**
 * Indian tax regime — Old (with deductions/exemptions) vs New (lower rates, fewer deductions).
 *
 * SPEC REF: FR-1.2 — "tax regime (old/new)"
 *
 * This affects tax calculations in Batch 5 (Tax Center).
 * For now, we store it per person so it's ready when the tax engine is built.
 */
public enum TaxRegime {
    OLD,    // Traditional regime with Section 80C, 80D, HRA etc. deductions
    NEW     // Simplified regime with lower tax slabs but fewer deductions
}
