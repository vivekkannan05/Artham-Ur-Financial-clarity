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
 * Income — A single income source for a person.
 *
 * Each person can have multiple income entries (salary + rental + dividends, etc.).
 * Each income entry has a type, amount, frequency, and time range.
 *
 * SPEC REF: FR-2.1 — "Income by person and period (monthly/yearly). Types include:
 * salary, rental, interest, dividends, bonuses, business income, capital gains, other."
 *
 * FINANCIAL PRECISION: All amounts use BigDecimal (per NFR-1.2).
 * Never use float or double for money — floating-point arithmetic loses precision.
 * Example: 0.1 + 0.2 = 0.30000000000000004 in double. BigDecimal avoids this.
 */
@Entity
@Table(name = "income")
public class Income extends BaseEntity {

    /**
     * The person who earns this income.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false)
    private IncomeType incomeType;

    /**
     * Amount per occurrence (per month, per year, etc. — depends on frequency).
     * Stored as BigDecimal with 2-decimal precision for currency (per NFR-1.2).
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private Frequency frequency;

    /**
     * When this income source started. Used for projection calculations.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * When this income source ends (null = ongoing).
     * Example: A contract job ends on a specific date.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Where this income comes from (e.g., "TCS", "Flat in Bandra", "SBI FD").
     */
    @Column(name = "source")
    private String source;

    // ─── Getters & Setters ───────────────────────────────────

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(IncomeType incomeType) {
        this.incomeType = incomeType;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
