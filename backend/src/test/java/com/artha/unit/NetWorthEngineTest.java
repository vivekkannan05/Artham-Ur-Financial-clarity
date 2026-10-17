package com.artha.unit;

import com.artha.engine.NetWorthEngine;
import com.artha.engine.NetWorthEngine.HoldingValue;
import com.artha.engine.NetWorthEngine.LiabilityValue;
import com.artha.engine.NetWorthEngine.NetWorthResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NetWorthEngine (Algorithm A-9).
 *
 * SPEC REF: FR-5.3 — "Total assets (by asset class), total liabilities, net worth."
 *
 * FORMULA:
 *   netWorth = Σ(holdings.currentValue) - Σ(liabilities.principalOutstanding)
 */
@DisplayName("A-9: Net Worth Engine")
class NetWorthEngineTest {

    @Test
    @DisplayName("Standard case: 3 holdings - 2 liabilities = positive net worth")
    void testStandardCase() {
        List<HoldingValue> holdings = List.of(
                new HoldingValue(new BigDecimal("2500000"), "EQUITY"),   // ₹25L in equity MFs
                new HoldingValue(new BigDecimal("800000"), "DEBT"),      // ₹8L in FDs
                new HoldingValue(new BigDecimal("300000"), "GOLD")       // ₹3L in SGBs
        );

        List<LiabilityValue> liabilities = List.of(
                new LiabilityValue(new BigDecimal("2000000")),  // ₹20L home loan
                new LiabilityValue(new BigDecimal("300000"))    // ₹3L car loan
        );

        NetWorthResult result = NetWorthEngine.compute(holdings, liabilities);

        assertEquals(new BigDecimal("3600000.00"), result.totalAssets());
        assertEquals(new BigDecimal("2300000.00"), result.totalLiabilities());
        assertEquals(new BigDecimal("1300000.00"), result.netWorth());

        // Asset class breakdown
        Map<String, BigDecimal> byClass = result.assetsByClass();
        assertEquals(new BigDecimal("2500000.00"), byClass.get("EQUITY"));
        assertEquals(new BigDecimal("800000.00"), byClass.get("DEBT"));
        assertEquals(new BigDecimal("300000.00"), byClass.get("GOLD"));
    }

    @Test
    @DisplayName("Negative net worth: liabilities exceed assets")
    void testNegativeNetWorth() {
        List<HoldingValue> holdings = List.of(
                new HoldingValue(new BigDecimal("500000"), "EQUITY")
        );
        List<LiabilityValue> liabilities = List.of(
                new LiabilityValue(new BigDecimal("2000000"))
        );

        NetWorthResult result = NetWorthEngine.compute(holdings, liabilities);

        assertEquals(new BigDecimal("-1500000.00"), result.netWorth());
    }

    @Test
    @DisplayName("No liabilities: net worth equals total assets")
    void testNoLiabilities() {
        List<HoldingValue> holdings = List.of(
                new HoldingValue(new BigDecimal("1000000"), "EQUITY"),
                new HoldingValue(new BigDecimal("500000"), "DEBT")
        );

        NetWorthResult result = NetWorthEngine.compute(holdings, List.of());

        assertEquals(new BigDecimal("1500000.00"), result.totalAssets());
        assertEquals(new BigDecimal("0.00"), result.totalLiabilities());
        assertEquals(new BigDecimal("1500000.00"), result.netWorth());
    }

    @Test
    @DisplayName("No holdings: net worth is negative (all liabilities)")
    void testNoHoldings() {
        List<LiabilityValue> liabilities = List.of(
                new LiabilityValue(new BigDecimal("500000"))
        );

        NetWorthResult result = NetWorthEngine.compute(List.of(), liabilities);

        assertEquals(new BigDecimal("0.00"), result.totalAssets());
        assertEquals(new BigDecimal("-500000.00"), result.netWorth());
    }

    @Test
    @DisplayName("Both empty: net worth is zero")
    void testBothEmpty() {
        NetWorthResult result = NetWorthEngine.compute(List.of(), List.of());

        assertEquals(new BigDecimal("0.00"), result.netWorth());
        assertTrue(result.assetsByClass().isEmpty());
    }

    @Test
    @DisplayName("Multiple holdings in same asset class → grouped correctly")
    void testAssetClassGrouping() {
        List<HoldingValue> holdings = List.of(
                new HoldingValue(new BigDecimal("1000000"), "EQUITY"),  // Fund A
                new HoldingValue(new BigDecimal("500000"), "EQUITY"),   // Fund B
                new HoldingValue(new BigDecimal("200000"), "GOLD")     // SGB
        );

        NetWorthResult result = NetWorthEngine.compute(holdings, List.of());

        assertEquals(new BigDecimal("1500000.00"), result.assetsByClass().get("EQUITY"));
        assertEquals(new BigDecimal("200000.00"), result.assetsByClass().get("GOLD"));
        assertEquals(2, result.assetsByClass().size());
    }

    @Test
    @DisplayName("Null holding values are safely ignored")
    void testNullValues() {
        List<HoldingValue> holdings = List.of(
                new HoldingValue(new BigDecimal("1000000"), "EQUITY"),
                new HoldingValue(null, "DEBT")
        );
        List<LiabilityValue> liabilities = List.of(
                new LiabilityValue(new BigDecimal("500000")),
                new LiabilityValue(null)
        );

        NetWorthResult result = NetWorthEngine.compute(holdings, liabilities);

        assertEquals(new BigDecimal("1000000.00"), result.totalAssets());
        assertEquals(new BigDecimal("500000.00"), result.totalLiabilities());
        assertEquals(new BigDecimal("500000.00"), result.netWorth());
    }
}
