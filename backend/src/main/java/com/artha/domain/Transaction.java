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
import java.util.UUID;

/**
 * Transaction — An immutable financial transaction record.
 *
 * IMMUTABILITY IS THE KEY DESIGN DECISION HERE.
 *
 * SPEC REF: FR-3.4 — "Transactions SHALL NOT be modified after creation.
 * Immutability SHALL be enforced at the database level: INSERT + SELECT only."
 *
 * WHY IMMUTABLE?
 *   Financial history must be trustworthy. If you could edit past transactions:
 *   - XIRR calculations would silently change
 *   - Tax reports would become unreliable
 *   - Audit trail would be broken
 *
 *   Instead of editing, you create a TransactionCorrection that references
 *   the original. The original record is preserved forever.
 *
 * APPLICATION ENFORCEMENT:
 *   - No update/delete API endpoints for transactions
 *   - Service layer rejects any modification attempt
 *
 * DATABASE ENFORCEMENT (production):
 *   - App DB user has only INSERT + SELECT on this table
 *   - REVOKE UPDATE, DELETE ON transaction FROM artha_app;
 */
@Entity
@Table(name = "transaction")
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "units", precision = 15, scale = 4)
    private BigDecimal units;

    @Column(name = "nav_price", precision = 15, scale = 4)
    private BigDecimal navPrice;

    @Column(name = "source_of_funds")
    private String sourceOfFunds;

    @Column(name = "notes")
    private String notes;

    /**
     * If this transaction is tied to a goal (SIP-to-Goal mapping per FR-4.5).
     * Nullable — not all transactions are goal-linked.
     */
    @Column(name = "goal_id")
    private UUID goalId;

    // ─── Getters & Setters ───────────────────────────────────

    public Holding getHolding() { return holding; }
    public void setHolding(Holding holding) { this.holding = holding; }

    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }

    public BigDecimal getNavPrice() { return navPrice; }
    public void setNavPrice(BigDecimal navPrice) { this.navPrice = navPrice; }

    public String getSourceOfFunds() { return sourceOfFunds; }
    public void setSourceOfFunds(String sourceOfFunds) { this.sourceOfFunds = sourceOfFunds; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }
}
