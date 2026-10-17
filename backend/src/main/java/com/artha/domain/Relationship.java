package com.artha.domain;

/**
 * Relationship of a person to the household head.
 *
 * SPEC REF: FR-1.1 — "Members include: primary user, spouse, children,
 * parents, and other dependents."
 *
 * WHY an enum?
 *   Fixed set of values. The spec explicitly lists these relationship types.
 *   Enums give compile-time safety — you can't accidentally store "Spoouse".
 *   Stored in the DB as a STRING (not ordinal) so values are readable.
 */
public enum Relationship {
    SELF,           // The primary household user
    SPOUSE,         // Has independent financial identity (per FR-1.2)
    CHILD,
    PARENT,
    SIBLING,
    DEPENDENT,      // Other dependents (e.g., in-laws, grandparents)
    OTHER
}
