package com.artha.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tax flag raised by the tax service.
 *
 * SPEC REF: FR-6.1 — Tax center with clubbing flags, EPF threshold flags, etc.
 * Each flag has a type, severity, affected person, and explanation.
 */
public record TaxFlag(
        String flagType,          // e.g., "CLUBBING", "EPF_EXCESS", "CAPITAL_GAINS"
        String severity,          // CRITICAL, WARNING, INFO
        UUID personId,
        String personName,
        BigDecimal amount,        // The amount involved
        String message            // Human-readable explanation
) {}
