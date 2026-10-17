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
 * TransactionCorrection — Adjustment to an immutable transaction.
 *
 * Since transactions can't be edited (per FR-3.4), corrections are
 * recorded separately with a reference to the original.
 *
 * SPEC REF: FR-3.4 — "Corrections are recorded as new compensating transactions.
 * A separate transaction_correction table records adjustments."
 */
@Entity
@Table(name = "transaction_correction")
public class TransactionCorrection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id", nullable = false)
    private Transaction originalTransaction;

    @Column(name = "correction_date", nullable = false)
    private LocalDate correctionDate;

    @Column(name = "corrected_amount", precision = 15, scale = 2)
    private BigDecimal correctedAmount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "correction_type", nullable = false)
    private CorrectionType correctionType;

    // ─── Getters & Setters ───────────────────────────────────

    public Transaction getOriginalTransaction() { return originalTransaction; }
    public void setOriginalTransaction(Transaction originalTransaction) { this.originalTransaction = originalTransaction; }

    public LocalDate getCorrectionDate() { return correctionDate; }
    public void setCorrectionDate(LocalDate correctionDate) { this.correctionDate = correctionDate; }

    public BigDecimal getCorrectedAmount() { return correctedAmount; }
    public void setCorrectedAmount(BigDecimal correctedAmount) { this.correctedAmount = correctedAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public CorrectionType getCorrectionType() { return correctionType; }
    public void setCorrectionType(CorrectionType correctionType) { this.correctionType = correctionType; }
}
