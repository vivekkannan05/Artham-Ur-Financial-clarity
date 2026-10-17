package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Liability — A debt obligation of the household.
 *
 * Examples: Home loan from SBI (₹50L @ 8.5%, EMI ₹43,391),
 * car loan from ICICI (₹8L @ 9%, EMI ₹16,616), credit card balance.
 *
 * SPEC REF: FR-5.1 — "Model liabilities with: type, lender, principal outstanding,
 * interest rate, EMI amount, tenure (start/end dates), prepayment history,
 * and computed interest/principal split per EMI. For mortgages, link to Property."
 *
 * KEY FINANCIAL CONCEPTS:
 *   - EMI (Equated Monthly Installment): Fixed monthly payment comprising
 *     principal + interest. Early EMIs are interest-heavy; later EMIs are
 *     principal-heavy (amortization schedule).
 *   - Principal Outstanding: The remaining balance on the loan. Decreases
 *     as EMIs are paid. Used in net-worth calculation (liability side).
 *   - Prepayment: Extra payment beyond EMI to reduce principal faster.
 *     Reduces total interest paid over the loan lifetime.
 *   - Interest Rate: Annual rate. Monthly rate = annual / 12.
 *
 * RELATIONSHIP TO OTHER ENTITIES:
 *   - Household: Every liability belongs to a household (who owes the debt).
 *   - Property: Mortgages link to a Property entity (Batch 5, FR-6.3).
 *     Until then, propertyId stays null.
 *   - CashFlow: EMI amounts feed into the debt-service ratio calculation.
 *   - NetWorth: principalOutstanding is subtracted from total assets.
 */
@Entity
@Table(name = "liability")
public class Liability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_type", nullable = false)
    private LiabilityType liabilityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LiabilityStatus status = LiabilityStatus.ACTIVE;

    @Column(name = "lender", nullable = false)
    private String lender;

    /**
     * Current outstanding principal balance.
     * This is what you still owe the lender.
     * Used in: net-worth calculation (liability side), payoff projections.
     */
    @Column(name = "principal_outstanding", precision = 15, scale = 2, nullable = false)
    private BigDecimal principalOutstanding;

    /**
     * Original loan amount (what was borrowed initially).
     * Kept for reference — helps compute how much has been repaid.
     */
    @Column(name = "original_principal", precision = 15, scale = 2)
    private BigDecimal originalPrincipal;

    /**
     * Annual interest rate (as a percentage, e.g., 8.50 means 8.50%).
     * Monthly rate = annualRate / 12.
     * For credit cards, this can be 36-42%.
     */
    @Column(name = "interest_rate", precision = 8, scale = 4, nullable = false)
    private BigDecimal interestRate;

    /**
     * Fixed monthly EMI amount.
     * For credit cards with no fixed EMI, this may be the minimum payment.
     * Used in: cash-flow debt-service ratio (FR-2.3), financial calendar (FR-7.4).
     */
    @Column(name = "emi_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal emiAmount;

    @Column(name = "tenure_start", nullable = false)
    private LocalDate tenureStart;

    @Column(name = "tenure_end")
    private LocalDate tenureEnd;

    /**
     * Total prepayment made (cumulative extra payments beyond regular EMIs).
     * Defaults to 0. Updated when prepayments are recorded.
     */
    @Column(name = "prepayment_total", precision = 15, scale = 2)
    private BigDecimal prepaymentTotal = BigDecimal.ZERO;

    /**
     * Link to property (for MORTGAGE type only). Nullable.
     * Property entity comes in Batch 5 (FR-6.3). Until then, this is null.
     * We store the raw UUID rather than a @ManyToOne so we don't need the
     * Property entity to exist yet.
     */
    @Column(name = "property_id")
    private java.util.UUID propertyId;

    @Column(name = "notes")
    private String notes;

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }

    public LiabilityType getLiabilityType() { return liabilityType; }
    public void setLiabilityType(LiabilityType liabilityType) { this.liabilityType = liabilityType; }

    public LiabilityStatus getStatus() { return status; }
    public void setStatus(LiabilityStatus status) { this.status = status; }

    public String getLender() { return lender; }
    public void setLender(String lender) { this.lender = lender; }

    public BigDecimal getPrincipalOutstanding() { return principalOutstanding; }
    public void setPrincipalOutstanding(BigDecimal principalOutstanding) { this.principalOutstanding = principalOutstanding; }

    public BigDecimal getOriginalPrincipal() { return originalPrincipal; }
    public void setOriginalPrincipal(BigDecimal originalPrincipal) { this.originalPrincipal = originalPrincipal; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public BigDecimal getEmiAmount() { return emiAmount; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }

    public LocalDate getTenureStart() { return tenureStart; }
    public void setTenureStart(LocalDate tenureStart) { this.tenureStart = tenureStart; }

    public LocalDate getTenureEnd() { return tenureEnd; }
    public void setTenureEnd(LocalDate tenureEnd) { this.tenureEnd = tenureEnd; }

    public BigDecimal getPrepaymentTotal() { return prepaymentTotal; }
    public void setPrepaymentTotal(BigDecimal prepaymentTotal) { this.prepaymentTotal = prepaymentTotal; }

    public java.util.UUID getPropertyId() { return propertyId; }
    public void setPropertyId(java.util.UUID propertyId) { this.propertyId = propertyId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
