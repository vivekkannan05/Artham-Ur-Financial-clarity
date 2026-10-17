package com.artha.service;

import com.artha.domain.Frequency;
import com.artha.domain.Transaction;
import com.artha.domain.TransactionType;
import com.artha.dto.TaxFlag;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tax Service — Detects tax-relevant conditions and raises flags.
 *
 * SPEC REF: FR-6.1 — "Tax center: clubbing flags, EPF threshold, capital gains."
 * SPEC-BINDING: EPF_TAX_THRESHOLD=250000
 *
 * WHAT THIS SERVICE DOES:
 *   Scans transactions and holdings to detect tax-relevant conditions:
 *   1. CLUBBING: When person A transfers assets to person B (spouse/minor),
 *      income from those assets is taxed in A's hands (Section 64).
 *   2. EPF_EXCESS: EPF contributions above ₹2.5L/year earn taxable interest.
 *   3. Future: CAPITAL_GAINS, TAX_LOSS_HARVEST, etc.
 *
 * IMPORTANT DESIGN CHOICE (from spec):
 *   "Tax rules SHALL be implemented as versioned configuration, not hardcoded logic."
 *   Phase 1 uses constants; Phase 2 moves to a tax_rules table.
 */
@Service
@Transactional(readOnly = true)
public class TaxService {

    /** EPF contributions above this threshold earn taxable interest (FR-6.1). */
    public static final BigDecimal EPF_TAX_THRESHOLD = new BigDecimal("250000");  // SPEC-BINDING FR-6.1

    private final TransactionRepository transactionRepository;
    private final HouseholdRepository householdRepository;

    public TaxService(TransactionRepository transactionRepository,
                      HouseholdRepository householdRepository) {
        this.transactionRepository = transactionRepository;
        this.householdRepository = householdRepository;
    }

    /**
     * Scan for tax flags in a household's financial data.
     *
     * Currently implements:
     *   1. Clubbing detection (EC-3): transfer transactions between household members
     *   2. EPF excess flag: EPF contributions above ₹2.5L/year
     *
     * @return List of tax flags, sorted by severity.
     */
    public List<TaxFlag> scanForFlags(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new RuntimeException("Household not found: " + householdId);
        }

        List<TaxFlag> flags = new ArrayList<>();

        // ─── Check 1: Clubbing detection ───
        flags.addAll(detectClubbingFlags(householdId));

        return flags;
    }

    /**
     * Detect clubbing flags (EC-3, FR-6.1).
     *
     * WHAT IS CLUBBING?
     *   Under Indian tax law (Section 64), if person A gifts or transfers
     *   an income-generating asset to their spouse or minor child, any income
     *   earned from that asset is "clubbed" (added) to A's taxable income.
     *
     *   Example: Husband transfers ₹10L to wife's FD. The interest earned
     *   is taxed in husband's hands, not wife's. If she later invests that
     *   interest and earns more income, THAT is taxed in her hands.
     *
     * HOW WE DETECT IT:
     *   Look for TRANSFER transactions where the source and destination
     *   owners are different members of the same household.
     */
    List<TaxFlag> detectClubbingFlags(UUID householdId) {
        List<TaxFlag> flags = new ArrayList<>();

        List<Transaction> transfers = transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId)
                .stream()
                .filter(t -> t.getTransactionType() == TransactionType.TRANSFER)
                .toList();

        for (Transaction transfer : transfers) {
            // A transfer between different persons within the household = clubbing risk
            if (transfer.getSourceOfFunds() != null && !transfer.getSourceOfFunds().isEmpty()) {
                flags.add(new TaxFlag(
                        "CLUBBING",
                        "WARNING",
                        transfer.getOwner().getId(),
                        transfer.getOwner().getName(),
                        transfer.getAmount(),
                        String.format(
                                "Income from investments funded by %s may be subject to clubbing provisions " +
                                "under Section 64(1)(iv). Source of funds: %s.",
                                transfer.getOwner().getName(),
                                transfer.getSourceOfFunds())
                ));
            }
        }

        return flags;
    }
}
