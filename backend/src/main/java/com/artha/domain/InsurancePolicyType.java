package com.artha.domain;

/**
 * Type of insurance policy.
 *
 * SPEC REF: FR-6.2 — "type (term life / health / property / vehicle / critical illness)."
 *
 * WHY THESE TYPES?
 *   Each type serves a different financial protection purpose:
 *   - TERM_LIFE: Pays lump sum on death. Cheapest per ₹ of cover. The spec checks
 *     "coverage < 10× income" as a gap (SPEC-BINDING: TERM_LIFE_MULTIPLIER=10).
 *   - HEALTH: Pays hospital bills. Spec checks "< ₹10L per family" as a gap.
 *   - PROPERTY: Protects home/building against damage/theft.
 *   - VEHICLE: Motor insurance (third-party is mandatory in India).
 *   - CRITICAL_ILLNESS: Lump sum on diagnosis of specified diseases (cancer, stroke, etc.).
 */
public enum InsurancePolicyType {
    TERM_LIFE,
    HEALTH,
    PROPERTY,
    VEHICLE,
    CRITICAL_ILLNESS
}
