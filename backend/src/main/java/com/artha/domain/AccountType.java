package com.artha.domain;

/**
 * Type of financial account.
 *
 * SPEC REF: FR-3.2 — "account type (demat, savings, PF, MF folio, etc.)"
 */
public enum AccountType {
    DEMAT,          // Stocks, ETFs, REITs, InvITs
    SAVINGS,        // Bank savings account
    PF,             // Provident Fund (EPF/VPF/PPF)
    MF_FOLIO,       // Mutual Fund folio
    NPS,            // National Pension System
    POST_OFFICE,    // SCSS, POMIS, KVP, NSC
    TRADING,        // F&O, crypto exchange
    INSURANCE,      // ULIP accounts
    OTHER
}
