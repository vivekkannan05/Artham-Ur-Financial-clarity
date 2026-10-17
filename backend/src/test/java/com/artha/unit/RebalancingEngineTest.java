package com.artha.unit;

import com.artha.engine.RebalancingEngine;
import com.artha.engine.RebalancingEngine.RebalanceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-11: Rebalancing within threshold (no action)
 * UT-12: Rebalancing with contribution redirect
 */
@DisplayName("UT-11/12: Rebalancing Engine")
class RebalancingEngineTest {

    @Test
    @DisplayName("UT-11: Within threshold → no rebalancing (48% actual vs 50% target)")
    void testWithinThreshold() {
        RebalanceResult result = RebalancingEngine.check(
                new BigDecimal("48.00"),
                new BigDecimal("50.00"),
                new BigDecimal("5.00"));

        assertFalse(result.rebalancingNeeded());
        assertEquals(new BigDecimal("2.00"), result.deviationPct());
    }

    @Test
    @DisplayName("UT-12: Over-allocated equity → redirect SIP to safe (60% vs 50%)")
    void testOverAllocatedEquity() {
        RebalanceResult result = RebalancingEngine.check(
                new BigDecimal("60.00"),
                new BigDecimal("50.00"),
                new BigDecimal("5.00"));

        assertTrue(result.rebalancingNeeded());
        assertEquals(new BigDecimal("10.00"), result.deviationPct());
        assertTrue(result.recommendation().contains("safe instruments"));
    }

    @Test
    @DisplayName("UT-12: Under-allocated equity → redirect SIP to equity (35% vs 50%)")
    void testUnderAllocatedEquity() {
        RebalanceResult result = RebalancingEngine.check(
                new BigDecimal("35.00"),
                new BigDecimal("50.00"),
                new BigDecimal("5.00"));

        assertTrue(result.rebalancingNeeded());
        assertEquals(new BigDecimal("15.00"), result.deviationPct());
        assertTrue(result.recommendation().contains("equity instruments"));
    }

    @Test
    @DisplayName("Exact threshold → not triggered (deviation = threshold)")
    void testExactThreshold() {
        RebalanceResult result = RebalancingEngine.check(
                new BigDecimal("55.00"),
                new BigDecimal("50.00"),
                new BigDecimal("5.00"));

        assertFalse(result.rebalancingNeeded());
    }
}
