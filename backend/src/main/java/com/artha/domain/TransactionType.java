package com.artha.domain;

/**
 * Type of financial transaction.
 *
 * SPEC REF: FR-3.4 — "type (buy/sell/SIP/dividend/interest/redemption/switch/bonus/split/transfer)"
 *
 * SIGN CONVENTION (for XIRR calculation):
 *   - Outflows (money leaving your pocket → into investment): BUY, SIP → negative cash flow
 *   - Inflows (money coming back to you): SELL, DIVIDEND, INTEREST, REDEMPTION → positive cash flow
 *   - Neutral (no money movement): BONUS, SPLIT → zero cash flow (but units change)
 *   - Complex: SWITCH (sell from A, buy into B) → two separate transactions
 */
public enum TransactionType {
    BUY,            // Purchase of units/shares
    SELL,           // Sale of units/shares
    SIP,            // Systematic Investment Plan (recurring buy)
    DIVIDEND,       // Dividend payout received
    INTEREST,       // Interest earned (FD, bonds, PPF)
    REDEMPTION,     // Full or partial redemption (mutual funds)
    SWITCH,         // Switch between funds (recorded as sell + buy pair)
    BONUS,          // Bonus units issued (no cash flow)
    SPLIT,          // Stock split (units change, no cash flow)
    TRANSFER        // Transfer between accounts
}
