package com.artha.unit;

import com.artha.engine.FundingProjectionEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-04: SIP future value projection
 * UT-05: SIP with annual step-up
 * UT-06: Goal inflation-adjusted target
 */
@DisplayName("UT-04/05/06: Funding Projection Engine")
class FundingProjectionEngineTest {

    @Test
    @DisplayName("UT-06: Inflation-adjusted target: ₹25L at 6% for 10 years ≈ ₹44.77L")
    void testInflationAdjustedTarget() {
        BigDecimal result = FundingProjectionEngine.inflationAdjustedTarget(
                new BigDecimal("2500000"), new BigDecimal("0.06"), 10);
        // 2500000 × 1.06^10 ≈ 4477120
        assertTrue(result.compareTo(new BigDecimal("4470000")) > 0);
        assertTrue(result.compareTo(new BigDecimal("4490000")) < 0);
    }

    @Test
    @DisplayName("UT-04: SIP FV: ₹10,000/month at 12% for 10 years")
    void testSipFutureValue() {
        BigDecimal result = FundingProjectionEngine.sipFutureValue(
                new BigDecimal("10000"), new BigDecimal("0.12"), 10);
        // Expected ≈ ₹23.23L (from financial calculator)
        assertTrue(result.compareTo(new BigDecimal("2300000")) > 0,
                "SIP FV should be > ₹23L, got " + result);
        assertTrue(result.compareTo(new BigDecimal("2400000")) < 0,
                "SIP FV should be < ₹24L, got " + result);
    }

    @Test
    @DisplayName("UT-04: SIP FV with 0% return = simple sum")
    void testSipFutureValueZeroReturn() {
        BigDecimal result = FundingProjectionEngine.sipFutureValue(
                new BigDecimal("10000"), BigDecimal.ZERO, 5);
        // 10000 × 60 months = 600000
        assertEquals(new BigDecimal("600000.00"), result);
    }

    @Test
    @DisplayName("UT-05: SIP with 10% step-up should exceed no-step-up FV")
    void testSipWithStepUp() {
        BigDecimal noStepUp = FundingProjectionEngine.sipFutureValue(
                new BigDecimal("10000"), new BigDecimal("0.12"), 10);
        BigDecimal withStepUp = FundingProjectionEngine.sipFutureValueWithStepUp(
                new BigDecimal("10000"), new BigDecimal("0.12"), 10, new BigDecimal("0.10"));

        assertTrue(withStepUp.compareTo(noStepUp) > 0,
                "Step-up FV should exceed no-step-up FV");
    }

    @Test
    @DisplayName("UT-05: SIP with 0% step-up = same as no step-up")
    void testSipWithZeroStepUp() {
        BigDecimal noStepUp = FundingProjectionEngine.sipFutureValue(
                new BigDecimal("10000"), new BigDecimal("0.12"), 10);
        BigDecimal zeroStepUp = FundingProjectionEngine.sipFutureValueWithStepUp(
                new BigDecimal("10000"), new BigDecimal("0.12"), 10, BigDecimal.ZERO);

        assertEquals(noStepUp, zeroStepUp);
    }

    @Test
    @DisplayName("Required SIP to close shortfall")
    void testRequiredMonthlySip() {
        // Need ₹10L in 5 years at 12% return
        BigDecimal sip = FundingProjectionEngine.requiredMonthlySip(
                new BigDecimal("1000000"), new BigDecimal("0.12"), 5);
        // Should be around ₹12,000-13,000/month
        assertTrue(sip.compareTo(new BigDecimal("11000")) > 0, "SIP > ₹11k, got " + sip);
        assertTrue(sip.compareTo(new BigDecimal("14000")) < 0, "SIP < ₹14k, got " + sip);
    }

    @Test
    @DisplayName("Corpus future value: ₹10L at 10% for 10 years ≈ ₹25.94L")
    void testCorpusFutureValue() {
        BigDecimal result = FundingProjectionEngine.corpusFutureValue(
                new BigDecimal("1000000"), new BigDecimal("0.10"), 10);
        assertTrue(result.compareTo(new BigDecimal("2590000")) > 0);
        assertTrue(result.compareTo(new BigDecimal("2600000")) < 0);
    }
}
