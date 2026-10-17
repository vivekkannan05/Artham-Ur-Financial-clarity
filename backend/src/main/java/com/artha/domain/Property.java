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
 * Property — A real estate asset owned by the household.
 *
 * SPEC REF: FR-6.3 — "Model properties with: purchase date, purchase cost,
 * current estimated value, linked mortgage, monthly rental income, monthly expenses,
 * rental yield (annual rent / current value), net equity (current value − mortgage)."
 *
 * WHY REAL ESTATE IS SPECIAL:
 *   Unlike financial instruments (MFs, FDs), property:
 *   1. Can generate income (rent) while also appreciating in value.
 *   2. Often has a linked liability (mortgage) — the net equity is what matters.
 *   3. Has ongoing expenses (maintenance, property tax, insurance).
 *   4. Is illiquid — you can't sell 10% of a house.
 *   5. Needs separate valuation (no daily NAV — manual estimate or market data).
 *
 * COMPUTED FIELDS (in the service layer, not stored):
 *   - rentalYield = (monthlyRentalIncome × 12) / currentValue × 100
 *   - netEquity = currentValue - mortgageOutstanding (from linked Liability)
 *   - annualNetIncome = (monthlyRentalIncome - monthlyExpenses) × 12
 */
@Entity
@Table(name = "property")
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 15, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "current_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentValue;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    /** Monthly rental income (₹0 if self-occupied). */
    @Column(name = "monthly_rental_income", precision = 15, scale = 2)
    private BigDecimal monthlyRentalIncome = BigDecimal.ZERO;

    /** Monthly expenses: maintenance + property tax + insurance prorated. */
    @Column(name = "monthly_expenses", precision = 15, scale = 2)
    private BigDecimal monthlyExpenses = BigDecimal.ZERO;

    @Column(name = "is_self_occupied")
    private boolean selfOccupied = true;

    @Column(name = "notes")
    private String notes;

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public void setPurchaseCost(BigDecimal purchaseCost) { this.purchaseCost = purchaseCost; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }

    public BigDecimal getMonthlyRentalIncome() { return monthlyRentalIncome; }
    public void setMonthlyRentalIncome(BigDecimal monthlyRentalIncome) { this.monthlyRentalIncome = monthlyRentalIncome; }

    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }

    public boolean isSelfOccupied() { return selfOccupied; }
    public void setSelfOccupied(boolean selfOccupied) { this.selfOccupied = selfOccupied; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
