package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * GoalAllocation — Maps a holding (or portion of it) to a goal.
 *
 * SPEC REF: FR-4.5 — Holding-to-goal mapping.
 *   A holding can be split across multiple goals (e.g., 80% retirement, 20% education).
 *   Allocations per holding must sum to 100%.
 *
 * COMPOSITE UNIQUE: (goal_id, holding_id) — one allocation per goal-holding pair.
 */
@Entity
@Table(name = "goal_allocation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"goal_id", "holding_id"})
})
public class GoalAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;

    /** Percentage of this holding allocated to this goal (0-100). */
    @Column(name = "allocation_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    // ─── Getters & Setters ───────────────────────────────────

    public Goal getGoal() { return goal; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public Holding getHolding() { return holding; }
    public void setHolding(Holding holding) { this.holding = holding; }
    public BigDecimal getAllocationPercentage() { return allocationPercentage; }
    public void setAllocationPercentage(BigDecimal allocationPercentage) { this.allocationPercentage = allocationPercentage; }
}
