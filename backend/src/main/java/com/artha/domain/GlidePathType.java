package com.artha.domain;

/**
 * Glide-path type: date-based (education/house) or age-based (retirement).
 * SPEC REF: FR-4.3
 */
public enum GlidePathType {
    DATE_BASED,     // Allocation shifts as goal DATE approaches
    AGE_BASED       // Allocation shifts as person AGES
}
