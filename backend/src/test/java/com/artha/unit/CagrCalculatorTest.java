package com.artha.unit;

import com.artha.engine.CagrCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-03: CAGR calculation tests.
 *
 * SPEC REF: FR-3.6, Test Plan UT-03
 * FORMULA: CAGR = (endValue / startValue) ^ (1/years) - 1
 */
@DisplayName("UT-03: CAGR Calculator")
class CagrCalculatorTest {

    @Test
    @DisplayName("Standard CAGR: ₹1L → ₹1.61L in 5 years = ~10%")
    void testStandardCagr() {
        // (161051 / 100000) ^ (1/5) - 1 ≈ 0.10
        Optional<BigDecimal> result = CagrCalculator.calculate(
                new BigDecimal("100000"),
                new BigDecimal("161051"),
                new BigDecimal("5")
        );

        assertTrue(result.isPresent());
        BigDecimal cagr = result.get();
        assertTrue(cagr.subtract(new BigDecimal("0.10")).abs()
                .compareTo(new BigDecimal("0.001")) < 0,
                "CAGR should be ~10%, got " + cagr);
    }

    @Test
    @DisplayName("Doubling in ~7.2 years = ~10% CAGR (Rule of 72)")
    void testRuleOf72() {
        Optional<BigDecimal> result = CagrCalculator.calculate(
                new BigDecimal("100000"),
                new BigDecimal("200000"),
                new BigDecimal("7.2")
        );

        assertTrue(result.isPresent());
        BigDecimal cagr = result.get();
        assertTrue(cagr.subtract(new BigDecimal("0.10")).abs()
                .compareTo(new BigDecimal("0.005")) < 0,
                "CAGR should be ~10% (Rule of 72), got " + cagr);
    }

    @Test
    @DisplayName("1 year: CAGR = simple return")
    void testOneYear() {
        // ₹100 → ₹120 in 1 year = 20%
        Optional<BigDecimal> result = CagrCalculator.calculate(
                new BigDecimal("100"),
                new BigDecimal("120"),
                new BigDecimal("1")
        );

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("0.200000"), result.get());
    }

    @Test
    @DisplayName("Negative return: value decreased")
    void testNegativeReturn() {
        Optional<BigDecimal> result = CagrCalculator.calculate(
                new BigDecimal("100000"),
                new BigDecimal("80000"),
                new BigDecimal("2")
        );

        assertTrue(result.isPresent());
        assertTrue(result.get().compareTo(BigDecimal.ZERO) < 0, "CAGR should be negative");
    }

    @Test
    @DisplayName("Zero start value → empty")
    void testZeroStartValue() {
        Optional<BigDecimal> result = CagrCalculator.calculate(
                BigDecimal.ZERO, new BigDecimal("100000"), new BigDecimal("5"));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Null inputs → empty")
    void testNullInputs() {
        assertTrue(CagrCalculator.calculate(null, new BigDecimal("100"), new BigDecimal("1")).isEmpty());
        assertTrue(CagrCalculator.calculate(new BigDecimal("100"), null, new BigDecimal("1")).isEmpty());
        assertTrue(CagrCalculator.calculate(new BigDecimal("100"), new BigDecimal("100"), null).isEmpty());
    }
}
