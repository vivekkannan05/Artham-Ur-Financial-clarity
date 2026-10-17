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
 * Expense — A spending entry for a person.
 *
 * SPEC REF: FR-2.2 — "Expenses with: category, amount, frequency, owner,
 * and a flag distinguishing consumption from investment outflow."
 *
 * The expense category (CONSUMPTION vs INVESTMENT_OUTFLOW) is critical for cash-flow:
 *   - Both reduce free cash flow
 *   - But savings rate calculation treats investment outflows differently
 *   - SIP payments and EMI principal are investment outflows — money is being allocated,
 *     not spent
 */
@Entity
@Table(name = "expense")
public class Expense extends BaseEntity {

    /**
     * The person who incurs this expense.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * What the expense is for (e.g., "Groceries", "SIP - Axis Bluechip", "House EMI").
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Amount per occurrence (per the frequency).
     * BigDecimal with 2-decimal precision (per NFR-1.2).
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private Frequency frequency;

    /**
     * CONSUMPTION vs INVESTMENT_OUTFLOW — critical for cash-flow calculations.
     * See ExpenseCategory enum for details.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * When recurring expense ends (null = ongoing).
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    // ─── Getters & Setters ───────────────────────────────────

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
