package com.artha.service;

import com.artha.domain.Holding;
import com.artha.domain.HoldingStatus;
import com.artha.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Stale Data Detection Service — Flags records not updated in > 90 days.
 *
 * SPEC REF: FR-7.7 — "Values not updated in 90 days SHALL be flagged as stale."
 * SPEC-BINDING: STALE_DATA_THRESHOLD_DAYS=90 (FR-7.7, NFR-2.2)
 *
 * WHAT "STALE" MEANS:
 *   Financial data that hasn't been updated in 90 days may no longer reflect
 *   reality. Holdings with manually entered NAVs, liabilities with estimated
 *   balances, or accounts with no recent transactions are candidates.
 *
 *   Flagging stale data is critical for trustworthiness — the system must NOT
 *   imply accuracy when values are manually entered and aging (NFR-2.2).
 *
 * HOW IT WORKS:
 *   Checks the BaseEntity.updatedAt field against the threshold.
 *   Returns a list of stale holdings with their last-update timestamps.
 *   Phase 1: Holdings only. Phase 2: Extends to liabilities, accounts, etc.
 */
@Service
@Transactional(readOnly = true)
public class StaleDataService {

    /** SPEC-BINDING FR-7.7 = STALE_DATA_THRESHOLD_DAYS=90 */
    public static final int STALE_THRESHOLD_DAYS = 90;

    private final HoldingRepository holdingRepository;

    public StaleDataService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    /**
     * Find active holdings that haven't been updated in > 90 days.
     */
    public List<StaleRecord> findStaleHoldings(UUID householdId) {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        List<Holding> active = holdingRepository
                .findByOwnerHouseholdIdAndStatus(householdId, HoldingStatus.ACTIVE);

        return active.stream()
                .filter(h -> h.getUpdatedAt() != null && h.getUpdatedAt().isBefore(cutoff))
                .map(h -> new StaleRecord(
                        h.getId(),
                        "HOLDING",
                        h.getName(),
                        h.getUpdatedAt(),
                        ChronoUnit.DAYS.between(h.getUpdatedAt(), Instant.now())
                ))
                .toList();
    }

    /**
     * A record flagged as stale (not updated in > 90 days).
     */
    public record StaleRecord(
            UUID entityId,
            String entityType,
            String name,
            Instant lastUpdated,
            long daysSinceUpdate
    ) {}
}
