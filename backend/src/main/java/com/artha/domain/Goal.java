package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal — A financial target the household is working toward.
 *
 * Examples: "Retire at 60 with ₹5 crore", "Aarav's education ₹25L by 2035",
 * "Emergency fund 6 months", "Dream house ₹1.5 crore by 2030".
 *
 * SPEC REF: FR-4.1 — Goal with name, type, target date, target amount,
 * inflation rate, priority, beneficiary, owner.
 */
@Entity
@Table(name = "goal")
public class Goal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    private GoalType goalType;

    /** Person who benefits from this goal (e.g., the child for education). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private Person beneficiary;

    /** Person funding this goal (may differ from beneficiary per FR-1.4). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Person owner;

    @Column(name = "target_date")
    private LocalDate targetDate;

    /** Target amount in today's money (before inflation adjustment). */
    @Column(name = "target_amount_today", precision = 15, scale = 2)
    private BigDecimal targetAmountToday;

    /** Assumed annual inflation rate (e.g., 0.06 = 6%). */
    @Column(name = "inflation_rate", precision = 10, scale = 6)
    private BigDecimal inflationRate;

    /** Expected annual return on investments allocated to this goal. */
    @Column(name = "expected_return_rate", precision = 10, scale = 6)
    private BigDecimal expectedReturnRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private GoalPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;

    /** Annual SIP step-up percentage (e.g., 0.10 = 10% annual increase). */
    @Column(name = "sip_step_up_pct", precision = 10, scale = 6)
    private BigDecimal sipStepUpPct;

    @OneToOne(mappedBy = "goal", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private GlidePath glidePath;

    @OneToMany(mappedBy = "goal", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<GoalAllocation> allocations = new ArrayList<>();

    // ─── Getters & Setters ───────────────────────────────────

    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }
    public Person getBeneficiary() { return beneficiary; }
    public void setBeneficiary(Person beneficiary) { this.beneficiary = beneficiary; }
    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public BigDecimal getTargetAmountToday() { return targetAmountToday; }
    public void setTargetAmountToday(BigDecimal targetAmountToday) { this.targetAmountToday = targetAmountToday; }
    public BigDecimal getInflationRate() { return inflationRate; }
    public void setInflationRate(BigDecimal inflationRate) { this.inflationRate = inflationRate; }
    public BigDecimal getExpectedReturnRate() { return expectedReturnRate; }
    public void setExpectedReturnRate(BigDecimal expectedReturnRate) { this.expectedReturnRate = expectedReturnRate; }
    public GoalPriority getPriority() { return priority; }
    public void setPriority(GoalPriority priority) { this.priority = priority; }
    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public BigDecimal getSipStepUpPct() { return sipStepUpPct; }
    public void setSipStepUpPct(BigDecimal sipStepUpPct) { this.sipStepUpPct = sipStepUpPct; }
    public GlidePath getGlidePath() { return glidePath; }
    public void setGlidePath(GlidePath glidePath) { this.glidePath = glidePath; }
    public List<GoalAllocation> getAllocations() { return allocations; }
    public void setAllocations(List<GoalAllocation> allocations) { this.allocations = allocations; }
}
