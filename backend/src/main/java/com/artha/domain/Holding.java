package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Holding — An ownership position in a financial instrument.
 *
 * Examples: 500 units of Axis Bluechip MF, 100 shares of TCS,
 * ₹5,00,000 in SBI FD, 10 grams of SGB.
 *
 * A Holding belongs to an Account and a Person (owner), and references
 * an InstrumentType that defines its behavior.
 *
 * SPEC REF: FR-3.3 — "Holdings: instrument type, account, owner, units,
 * acquisition cost, current value, status."
 *
 * LIFECYCLE (per EC-10):
 *   ACTIVE → transactions flow in/out
 *   CLOSED → zero units after full redemption, retained for historical XIRR
 *   MATURED → fixed-term instrument reached maturity
 */
@Entity
@Table(name = "holding")
public class Holding extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_type_id", nullable = false)
    private InstrumentType instrumentType;

    @Column(name = "units", precision = 15, scale = 4)
    private BigDecimal units;

    @Column(name = "acquisition_cost_total", precision = 15, scale = 2)
    private BigDecimal acquisitionCostTotal;

    @Column(name = "acquisition_cost_per_unit", precision = 15, scale = 4)
    private BigDecimal acquisitionCostPerUnit;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HoldingStatus status = HoldingStatus.ACTIVE;

    /** Source of this data: MANUAL, CSV_IMPORT, CAS_IMPORT (per NFR-2.2) */
    @Column(name = "provenance_source")
    private String provenanceSource;

    @Column(name = "provenance_date")
    private LocalDate provenanceDate;

    /** Data confidence: HIGH (direct import), MEDIUM (CSV), LOW (manual) */
    @Column(name = "provenance_confidence")
    private String provenanceConfidence;

    @OneToMany(mappedBy = "holding")
    private List<Transaction> transactions = new ArrayList<>();

    // ─── Getters & Setters ───────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }

    public InstrumentType getInstrumentType() { return instrumentType; }
    public void setInstrumentType(InstrumentType instrumentType) { this.instrumentType = instrumentType; }

    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }

    public BigDecimal getAcquisitionCostTotal() { return acquisitionCostTotal; }
    public void setAcquisitionCostTotal(BigDecimal acquisitionCostTotal) { this.acquisitionCostTotal = acquisitionCostTotal; }

    public BigDecimal getAcquisitionCostPerUnit() { return acquisitionCostPerUnit; }
    public void setAcquisitionCostPerUnit(BigDecimal acquisitionCostPerUnit) { this.acquisitionCostPerUnit = acquisitionCostPerUnit; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public HoldingStatus getStatus() { return status; }
    public void setStatus(HoldingStatus status) { this.status = status; }

    public String getProvenanceSource() { return provenanceSource; }
    public void setProvenanceSource(String provenanceSource) { this.provenanceSource = provenanceSource; }

    public LocalDate getProvenanceDate() { return provenanceDate; }
    public void setProvenanceDate(LocalDate provenanceDate) { this.provenanceDate = provenanceDate; }

    public String getProvenanceConfidence() { return provenanceConfidence; }
    public void setProvenanceConfidence(String provenanceConfidence) { this.provenanceConfidence = provenanceConfidence; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}
