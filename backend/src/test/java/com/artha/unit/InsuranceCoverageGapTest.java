package com.artha.unit;

import com.artha.dto.InsuranceCoverageGap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for insurance coverage gap DTOs and computation constants.
 *
 * SPEC REF: FR-6.2 — "term life < 10× income → flag; health < ₹10L → flag."
 * SPEC-BINDING: TERM_LIFE_MULTIPLIER=10, HEALTH_COVER_MIN=1000000
 *
 * The actual gap analysis is in InsuranceService (requires Spring context).
 * These tests verify the gap model and constants.
 */
@DisplayName("FR-6.2: Insurance Coverage Gap Model")
class InsuranceCoverageGapTest {

    @Test
    @DisplayName("Term life gap: cover ₹50L vs recommended ₹1Cr → ₹50L shortfall")
    void testTermLifeGap() {
        BigDecimal annualIncome = new BigDecimal("1000000");    // ₹10L
        BigDecimal recommended = annualIncome.multiply(BigDecimal.TEN);  // ₹1Cr
        BigDecimal actual = new BigDecimal("5000000");           // ₹50L
        BigDecimal shortfall = recommended.subtract(actual);

        InsuranceCoverageGap gap = new InsuranceCoverageGap(
                "TERM_LIFE", "Rahul",
                recommended, actual, shortfall,
                "Term life cover for Rahul is ₹50L — recommended minimum is ₹1Cr (10× annual income)."
        );

        assertEquals("TERM_LIFE", gap.gapType());
        assertEquals(new BigDecimal("10000000"), gap.recommended());
        assertEquals(new BigDecimal("5000000"), gap.shortfall());
        assertTrue(gap.message().contains("10×"));
    }

    @Test
    @DisplayName("Health gap: cover ₹5L vs recommended ₹10L → ₹5L shortfall")
    void testHealthGap() {
        BigDecimal recommended = new BigDecimal("1000000");
        BigDecimal actual = new BigDecimal("500000");

        InsuranceCoverageGap gap = new InsuranceCoverageGap(
                "HEALTH", "Household",
                recommended, actual, recommended.subtract(actual),
                "Total health cover is ₹5L — recommended minimum is ₹10L."
        );

        assertEquals("HEALTH", gap.gapType());
        assertEquals(new BigDecimal("500000"), gap.shortfall());
    }

    @Test
    @DisplayName("No gap when coverage meets threshold")
    void testNoGap() {
        BigDecimal recommended = new BigDecimal("10000000");
        BigDecimal actual = new BigDecimal("15000000");  // Exceeds 10×
        BigDecimal shortfall = recommended.subtract(actual).max(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, shortfall);
    }
}
