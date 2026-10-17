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
 * InsurancePolicy — A protection policy held by a household member.
 *
 * Examples: ICICI Pru iProtect Smart (term life, ₹1Cr cover, ₹12,000/yr),
 * Star Health Comprehensive (health, ₹10L cover, ₹18,000/yr).
 *
 * SPEC REF: FR-6.2 — "Track insurance policies: type, insured person, owner,
 * sum assured, premium, due date, start/end dates, renewal status."
 *
 * COVERAGE GAP ANALYSIS (also FR-6.2):
 *   The system evaluates two basic gaps:
 *   (a) Term life cover < 10× annual income → flag
 *   (b) Health cover < ₹10L per family → flag
 *   These checks are performed by InsuranceService, not in the entity itself.
 *
 * WHY SEPARATE insuredPerson AND owner?
 *   In Indian families, the policy owner and insured person can differ:
 *   - Parent buys health policy covering the whole family
 *   - Husband buys term life where he's both owner and insured
 *   - Employer provides group health (employer owns, employee is insured)
 *   The owner pays premiums (tax deduction 80D); the insured person gets coverage.
 */
@Entity
@Table(name = "insurance_policy")
public class InsurancePolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private InsurancePolicyType policyType;

    /** The person who is covered by this policy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insured_person_id", nullable = false)
    private Person insuredPerson;

    /** The person who owns (and pays premiums for) this policy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @Column(name = "policy_name")
    private String policyName;

    @Column(name = "insurer", nullable = false)
    private String insurer;

    @Column(name = "policy_number")
    private String policyNumber;

    /**
     * Sum assured / coverage amount.
     * For term life: death benefit (e.g., ₹1,00,00,000).
     * For health: maximum claim amount per year (e.g., ₹10,00,000).
     */
    @Column(name = "sum_assured", precision = 15, scale = 2, nullable = false)
    private BigDecimal sumAssured;

    @Column(name = "annual_premium", precision = 15, scale = 2, nullable = false)
    private BigDecimal annualPremium;

    @Column(name = "premium_due_date")
    private LocalDate premiumDueDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "renewal_status", nullable = false)
    private RenewalStatus renewalStatus = RenewalStatus.ACTIVE;

    @Column(name = "notes")
    private String notes;

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }

    public InsurancePolicyType getPolicyType() { return policyType; }
    public void setPolicyType(InsurancePolicyType policyType) { this.policyType = policyType; }

    public Person getInsuredPerson() { return insuredPerson; }
    public void setInsuredPerson(Person insuredPerson) { this.insuredPerson = insuredPerson; }

    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getInsurer() { return insurer; }
    public void setInsurer(String insurer) { this.insurer = insurer; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public BigDecimal getSumAssured() { return sumAssured; }
    public void setSumAssured(BigDecimal sumAssured) { this.sumAssured = sumAssured; }

    public BigDecimal getAnnualPremium() { return annualPremium; }
    public void setAnnualPremium(BigDecimal annualPremium) { this.annualPremium = annualPremium; }

    public LocalDate getPremiumDueDate() { return premiumDueDate; }
    public void setPremiumDueDate(LocalDate premiumDueDate) { this.premiumDueDate = premiumDueDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public RenewalStatus getRenewalStatus() { return renewalStatus; }
    public void setRenewalStatus(RenewalStatus renewalStatus) { this.renewalStatus = renewalStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
