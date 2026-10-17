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
import java.util.ArrayList;
import java.util.List;

/**
 * Account — A financial account at an institution.
 *
 * Examples: Groww demat account, SBI savings account, EPFO PF account,
 * Axis MF folio, NPS account, post office account.
 *
 * An account belongs to a Person (the owner) and contains Holdings.
 *
 * SPEC REF: FR-3.2 — "Account with: owner, institution, account number (masked),
 * account type, currency (default INR), status."
 */
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    /**
     * Account number — stored masked for security (e.g., "****4567").
     * The full number is not stored in Artha.
     */
    @Column(name = "account_number_masked")
    private String accountNumberMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    /**
     * Currency code (ISO 4217). Default INR.
     * Supports multi-currency per NFR-6.2.
     */
    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "account")
    private List<Holding> holdings = new ArrayList<>();

    // ─── Getters & Setters ───────────────────────────────────

    public Person getOwner() { return owner; }
    public void setOwner(Person owner) { this.owner = owner; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getAccountNumberMasked() { return accountNumberMasked; }
    public void setAccountNumberMasked(String accountNumberMasked) { this.accountNumberMasked = accountNumberMasked; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public List<Holding> getHoldings() { return holdings; }
    public void setHoldings(List<Holding> holdings) { this.holdings = holdings; }
}
