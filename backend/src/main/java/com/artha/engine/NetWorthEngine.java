package com.artha.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Net Worth Engine — Computes a household's financial position.
 *
 * SPEC REF: FR-5.3 — "Calculate net-worth history: total assets (by asset class),
 * total liabilities, net worth."
 *
 * ALGORITHM (Design Doc A-9):
 *   totalAssets      = Σ(holding.currentValue) for all ACTIVE holdings
 *   totalLiabilities = Σ(liability.principalOutstanding) for all ACTIVE liabilities
 *   netWorth         = totalAssets - totalLiabilities
 *
 * Time complexity: O(h + l) where h = holdings, l = liabilities.
 * Space complexity: O(c) where c = number of asset classes (typically 5-8).
 *
 * WHY PURE STATIC?
 *   Same as other Artha engines — stateless computation.
 *   The service layer collects the data; this engine does the math.
 *
 * INPUT FORMAT:
 *   Rather than passing full entity objects (which would couple the engine
 *   to JPA), we use simple inner records as inputs. This makes the engine
 *   testable without any database or Spring context.
 */
public final class NetWorthEngine {

    private NetWorthEngine() {
        // Static utility — no instantiation
    }

    /**
     * A simplified view of a holding for net-worth calculation.
     * Only the fields we need: currentValue and asset class.
     *
     * @param currentValue  The current market value of this holding.
     * @param assetClass    The asset class (e.g., "EQUITY", "DEBT", "GOLD").
     */
    public record HoldingValue(BigDecimal currentValue, String assetClass) {}

    /**
     * A simplified view of a liability for net-worth calculation.
     *
     * @param principalOutstanding What is still owed on this debt.
     */
    public record LiabilityValue(BigDecimal principalOutstanding) {}

    /**
     * The result of a net-worth computation.
     *
     * @param totalAssets      Sum of all active holdings.
     * @param totalLiabilities Sum of all active liabilities.
     * @param netWorth         Assets minus liabilities.
     * @param assetsByClass    Breakdown: {"EQUITY": 2500000, "DEBT": 800000, ...}
     */
    public record NetWorthResult(
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal netWorth,
            Map<String, BigDecimal> assetsByClass
    ) {}

    /**
     * Compute net worth for a household.
     *
     * @param holdings    All ACTIVE holdings with their currentValue and assetClass.
     * @param liabilities All ACTIVE liabilities with their principalOutstanding.
     * @return NetWorthResult with total assets, liabilities, net worth, and class breakdown.
     */
    public static NetWorthResult compute(List<HoldingValue> holdings,
                                          List<LiabilityValue> liabilities) {

        // ─── Total Assets: sum of all holding values ───
        // Using streams for functional style (Design Doc says "Parallel streams"
        // but for small lists the overhead of parallelStream() isn't worth it.
        // We use sequential stream here; switch to parallel if h > 10,000).
        BigDecimal totalAssets = holdings.stream()
                .map(HoldingValue::currentValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Assets by Class: group and sum per asset class ───
        // Produces: {"EQUITY": 2500000.00, "DEBT": 800000.00, "GOLD": 300000.00}
        Map<String, BigDecimal> assetsByClass = holdings.stream()
                .filter(h -> h.currentValue() != null && h.assetClass() != null)
                .collect(Collectors.groupingBy(
                        HoldingValue::assetClass,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                HoldingValue::currentValue,
                                BigDecimal::add
                        )
                ));
        // Scale each class total to 2 decimal places
        assetsByClass.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));

        // ─── Total Liabilities: sum of outstanding principals ───
        BigDecimal totalLiabilities = liabilities.stream()
                .map(LiabilityValue::principalOutstanding)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Net Worth: the bottom line ───
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities)
                .setScale(2, RoundingMode.HALF_UP);

        return new NetWorthResult(totalAssets, totalLiabilities, netWorth, assetsByClass);
    }
}
