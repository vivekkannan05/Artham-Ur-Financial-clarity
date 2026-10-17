package com.artha.unit;

import com.artha.domain.EmergencyFundAdequacy;
import com.artha.dto.EmergencyFundStatus;
import com.artha.engine.EmergencyFundCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-13: Emergency fund adequate (8 months covered, target 6).
 * UT-14: Emergency fund critical (1 month covered, target 6).
 *
 * SPEC REF: FR-5.2 — Emergency fund adequacy.
 * SPEC-BINDING: EMERGENCY_FUND_MONTHS=6
 *
 * The tests use pure static methods — no Spring context needed.
 * We pass pre-computed monthlyExpenses and liquidReserve directly.
 */
@DisplayName("UT-13/14: Emergency Fund Calculator")
class EmergencyFundCalculatorTest {

    @Test
    @DisplayName("UT-13: Adequate — 8 months covered (target 6)")
    void testAdequate() {
        // Monthly essential expenses: ₹60,000
        // Liquid reserve: ₹4,80,000 (8 months × ₹60k)
        // Target: 6 months
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("60000"),
                new BigDecimal("480000"),
                6
        );

        assertEquals(EmergencyFundAdequacy.ADEQUATE, result.adequacy());
        assertEquals(new BigDecimal("8.00"), result.monthsCovered());
        assertEquals(new BigDecimal("360000.00"), result.requiredReserve());
        assertEquals(BigDecimal.ZERO.setScale(2), result.shortfall());
    }

    @Test
    @DisplayName("UT-13: Adequate — exactly at target (6 months)")
    void testAdequateExactly() {
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("50000"),
                new BigDecimal("300000"),  // exactly 6 months
                6
        );

        assertEquals(EmergencyFundAdequacy.ADEQUATE, result.adequacy());
        assertEquals(new BigDecimal("6.00"), result.monthsCovered());
        assertEquals(BigDecimal.ZERO.setScale(2), result.shortfall());
    }

    @Test
    @DisplayName("UT-14: Critical — 1 month covered (target 6)")
    void testCritical() {
        // Monthly essential expenses: ₹60,000
        // Liquid reserve: ₹60,000 (1 month)
        // Target: 6 months
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("60000"),
                new BigDecimal("60000"),
                6
        );

        assertEquals(EmergencyFundAdequacy.CRITICAL, result.adequacy());
        assertEquals(new BigDecimal("1.00"), result.monthsCovered());
        // Shortfall = 360000 - 60000 = 300000
        assertEquals(new BigDecimal("300000.00"), result.shortfall());
    }

    @Test
    @DisplayName("UT-14: Critical — zero liquid reserve")
    void testCriticalZeroReserve() {
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("60000"),
                BigDecimal.ZERO,
                6
        );

        assertEquals(EmergencyFundAdequacy.CRITICAL, result.adequacy());
        assertEquals(new BigDecimal("0.00"), result.monthsCovered());
        assertEquals(new BigDecimal("360000.00"), result.shortfall());
    }

    @Test
    @DisplayName("Partial — 4 months covered (target 6)")
    void testPartial() {
        // 4 months is >= 3 (half of 6) but < 6
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("60000"),
                new BigDecimal("240000"),  // 4 months
                6
        );

        assertEquals(EmergencyFundAdequacy.PARTIAL, result.adequacy());
        assertEquals(new BigDecimal("4.00"), result.monthsCovered());
        assertEquals(new BigDecimal("120000.00"), result.shortfall());
    }

    @Test
    @DisplayName("Partial — exactly at half target (3 months)")
    void testPartialExactHalf() {
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                new BigDecimal("60000"),
                new BigDecimal("180000"),  // exactly 3 months
                6
        );

        assertEquals(EmergencyFundAdequacy.PARTIAL, result.adequacy());
    }

    @Test
    @DisplayName("Edge: zero expenses → always adequate")
    void testZeroExpenses() {
        EmergencyFundStatus result = EmergencyFundCalculator.compute(
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                6
        );

        assertEquals(EmergencyFundAdequacy.ADEQUATE, result.adequacy());
        assertEquals(BigDecimal.ZERO, result.shortfall());
    }
}
