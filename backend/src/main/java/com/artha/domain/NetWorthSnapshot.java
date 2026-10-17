package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * NetWorthSnapshot — A point-in-time record of a household's net worth.
 *
 * SPEC REF: FR-5.3 — "Calculate and store net-worth history at monthly and yearly
 * granularity: total assets (by asset class), total liabilities, net worth, and
 * month-over-month / year-over-year change. History is append-only;
 * past snapshots are never modified."
 *
 * WHY APPEND-ONLY?
 *   Net-worth snapshots are a time-series record. Each snapshot captures
 *   the household's financial state at a moment in time. If we allowed edits,
 *   we'd lose the historical trail — like rewriting entries in an accounting ledger.
 *   This design also enables trend charts: "My net worth was ₹45L in Jan, ₹48L
 *   in Feb, ₹52L in Mar" without worrying about retroactive changes.
 *
 * WHAT'S STORED:
 *   - totalAssets: Sum of all active holdings' currentValue
 *   - totalLiabilities: Sum of all active liabilities' principalOutstanding
 *   - netWorth: totalAssets - totalLiabilities
 *   - assetsByClassJson: JSONB breakdown like {"EQUITY": 2500000, "DEBT": 800000, "GOLD": 300000}
 *   - monthOverMonthChange: Net-worth change vs previous month's snapshot
 *   - yearOverYearChange: Net-worth change vs 12-months-ago snapshot
 *
 * ALGORITHM (Design Doc A-9):
 *   netWorth = Σ(holding.currentValue for ACTIVE holdings) - Σ(liability.principalOutstanding for ACTIVE liabilities)
 *   Time complexity: O(h + l) where h = holdings, l = liabilities
 */
@Entity
@Table(name = "net_worth_snapshot")
public class NetWorthSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_assets", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalLiabilities;

    @Column(name = "net_worth", precision = 15, scale = 2, nullable = false)
    private BigDecimal netWorth;

    /**
     * JSONB breakdown of assets by class.
     * Example: {"EQUITY": 2500000.00, "DEBT": 800000.00, "GOLD": 300000.00}
     * Enables the dashboard pie chart without re-querying holdings.
     */
    @Column(name = "assets_by_class_json", columnDefinition = "jsonb")
    private String assetsByClassJson;

    /**
     * Net-worth change compared to the previous month's snapshot.
     * Example: +150000.00 means net worth grew by ₹1.5L this month.
     * Null for the very first snapshot (no prior month to compare).
     */
    @Column(name = "month_over_month_change", precision = 15, scale = 2)
    private BigDecimal monthOverMonthChange;

    /**
     * Net-worth change compared to the same month last year.
     * Example: +500000.00 means net worth grew by ₹5L year-over-year.
     * Null if there's no snapshot from 12 months ago.
     */
    @Column(name = "year_over_year_change", precision = 15, scale = 2)
    private BigDecimal yearOverYearChange;

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }

    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }

    public BigDecimal getNetWorth() { return netWorth; }
    public void setNetWorth(BigDecimal netWorth) { this.netWorth = netWorth; }

    public String getAssetsByClassJson() { return assetsByClassJson; }
    public void setAssetsByClassJson(String assetsByClassJson) { this.assetsByClassJson = assetsByClassJson; }

    public BigDecimal getMonthOverMonthChange() { return monthOverMonthChange; }
    public void setMonthOverMonthChange(BigDecimal monthOverMonthChange) { this.monthOverMonthChange = monthOverMonthChange; }

    public BigDecimal getYearOverYearChange() { return yearOverYearChange; }
    public void setYearOverYearChange(BigDecimal yearOverYearChange) { this.yearOverYearChange = yearOverYearChange; }
}
