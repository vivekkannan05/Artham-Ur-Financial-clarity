package com.artha.domain;

/**
 * Type of liability (debt obligation).
 *
 * SPEC REF: FR-5.1 — "Liabilities with: type (mortgage / personal loan / car loan
 * / credit card / education loan / other)."
 *
 * WHY THESE TYPES?
 *   Each type has different characteristics in Indian personal finance:
 *   - MORTGAGE: Longest tenure (15-30yr), tax benefit on principal (80C) + interest (24b)
 *   - PERSONAL_LOAN: High interest (10-18%), no tax benefit, usually 1-5yr
 *   - CAR_LOAN: Medium interest, depreciating asset (unlike mortgage), 3-7yr
 *   - CREDIT_CARD: Extremely high interest (36-42%/yr), revolving, no fixed tenure
 *   - EDUCATION_LOAN: Tax benefit under 80E on interest, usually 5-15yr
 *   - OTHER: Loans from family, gold loans, etc.
 */
public enum LiabilityType {
    MORTGAGE,
    PERSONAL_LOAN,
    CAR_LOAN,
    CREDIT_CARD,
    EDUCATION_LOAN,
    OTHER
}
