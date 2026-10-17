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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Person — A member of a household.
 *
 * Represents any individual: the primary user, their spouse, children, parents, dependents.
 * Each person has their own financial identity: income sources, tax profile, and (in later batches)
 * accounts, holdings, and insurance policies.
 *
 * SPEC REFS:
 *   FR-1.1 — Members of a household with defined relationships
 *   FR-1.2 — Person data: name, DOB, gender, PAN, tax regime, etc.
 *             Spouse has independent financial identity.
 *   FR-1.5 — Source-of-funds tracking (for clubbing detection)
 *
 * SECURITY (per Review S-7):
 *   - PAN is encrypted at rest using AES-256 (panEncrypted column stores ciphertext)
 *   - PAN is masked in API responses (e.g., "ABCD****4F")
 *   - PAN is NEVER included in log output
 */
@Entity
@Table(name = "person")
public class Person extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Relationship to the household head (SELF, SPOUSE, CHILD, etc.).
     * Stored as string in DB for readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false)
    private Relationship relationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    /**
     * PAN number — encrypted at rest using AES-256.
     * This column stores the ciphertext, NOT the plaintext PAN.
     *
     * The PanEncryptor utility (in config/) handles encryption/decryption.
     * API responses must return the masked version (e.g., "ABCD****4F").
     *
     * SPEC REF: FR-1.2 (PAN encrypted at rest), Review S-7
     */
    @Column(name = "pan_encrypted")
    private String panEncrypted;

    /**
     * Tax regime — OLD (with deductions) or NEW (simplified).
     * Affects tax calculations in Batch 5.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime")
    private TaxRegime taxRegime;

    @Column(name = "residency_status")
    private String residencyStatus;

    @Column(name = "citizenship")
    private String citizenship;

    /**
     * The household this person belongs to.
     *
     * @ManyToOne: Many persons belong to one household.
     * FetchType.LAZY: Don't load the household unless explicitly accessed.
     *   This prevents N+1 query problems when loading a list of persons.
     *
     * @JoinColumn: This person table has a 'household_id' FK column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    /**
     * Income entries for this person.
     * Added in Batch 1 — a person can have multiple income sources.
     */
    @OneToMany(mappedBy = "person", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Income> incomes = new ArrayList<>();

    /**
     * Expense entries for this person.
     */
    @OneToMany(mappedBy = "person", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    // ─── Getters & Setters ───────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPanEncrypted() {
        return panEncrypted;
    }

    public void setPanEncrypted(String panEncrypted) {
        this.panEncrypted = panEncrypted;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public void setTaxRegime(TaxRegime taxRegime) {
        this.taxRegime = taxRegime;
    }

    public String getResidencyStatus() {
        return residencyStatus;
    }

    public void setResidencyStatus(String residencyStatus) {
        this.residencyStatus = residencyStatus;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public Household getHousehold() {
        return household;
    }

    public void setHousehold(Household household) {
        this.household = household;
    }

    public List<Income> getIncomes() {
        return incomes;
    }

    public void setIncomes(List<Income> incomes) {
        this.incomes = incomes;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }
}
