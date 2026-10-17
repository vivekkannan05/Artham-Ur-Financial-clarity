package com.artha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Net Worth Response — The household's financial position at a point in time.
 *
 * SPEC REF: FR-5.3 — "Total assets (by asset class), total liabilities, net worth,
 * and month-over-month / year-over-year change."
 *
 * FORMULA (Design Doc A-9):
 *   totalAssets = Σ(holding.currentValue) for all ACTIVE holdings
 *   totalLiabilities = Σ(liability.principalOutstanding) for all ACTIVE liabilities
 *   netWorth = totalAssets - totalLiabilities
 *
 * assetsByClass example: {"EQUITY": 2500000.00, "DEBT": 800000.00, "GOLD": 300000.00}
 *
 * WHY TRACK CHANGES (MoM/YoY)?
 *   Raw net worth alone doesn't tell you if you're progressing. A household
 *   with ₹50L net worth growing at ₹5L/year is healthier than one at ₹80L
 *   but shrinking at ₹2L/year. The changes make trends visible.
 */
public record NetWorthResponse(
        UUID householdId,
        LocalDate snapshotDate,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal netWorth,
        Map<String, BigDecimal> assetsByClass,
        BigDecimal monthOverMonthChange,
        BigDecimal yearOverYearChange
) {}
