package com.artha.service;

import com.artha.config.ArthaProperties;
import com.artha.domain.Holding;
import com.artha.domain.HoldingStatus;
import com.artha.domain.Household;
import com.artha.domain.Liability;
import com.artha.domain.LiabilityStatus;
import com.artha.domain.NetWorthSnapshot;
import com.artha.dto.EmergencyFundStatus;
import com.artha.dto.NetWorthResponse;
import com.artha.engine.EmergencyFundCalculator;
import com.artha.engine.NetWorthEngine;
import com.artha.engine.NetWorthEngine.HoldingValue;
import com.artha.engine.NetWorthEngine.LiabilityValue;
import com.artha.engine.NetWorthEngine.NetWorthResult;
import com.artha.repository.HoldingRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.LiabilityRepository;
import com.artha.repository.NetWorthSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Net Worth Service — Computes current net worth and manages snapshot history.
 *
 * SPEC REF: FR-5.3 — "Calculate and store net-worth history at monthly and yearly
 * granularity. History is append-only; past snapshots are never modified."
 *
 * SPEC REF: FR-5.2 — Emergency fund calculation also lives here because it needs
 * access to holdings data (to find liquid reserves) and expense data.
 *
 * This service orchestrates:
 *   1. NetWorthEngine (pure computation, Algorithm A-9)
 *   2. EmergencyFundCalculator (pure computation, Algorithm A-10)
 *   3. NetWorthSnapshotRepository (persistence)
 *
 * HOW SNAPSHOTS WORK:
 *   1. User (or scheduled job) calls POST /api/households/{id}/networth/snapshot
 *   2. We compute current net worth using live holdings + liabilities data
 *   3. We compare with previous snapshot for MoM/YoY changes
 *   4. We save a new append-only snapshot (unique per household + date)
 *   5. Dashboard reads the snapshot history for trend charts
 */
@Service
@Transactional(readOnly = true)
public class NetWorthService {

    private final HoldingRepository holdingRepository;
    private final LiabilityRepository liabilityRepository;
    private final NetWorthSnapshotRepository snapshotRepository;
    private final HouseholdRepository householdRepository;
    private final CashFlowService cashFlowService;
    private final ArthaProperties arthaProperties;
    private final ObjectMapper objectMapper;

    /** Instrument type codes considered "liquid" for emergency fund (FR-5.2). */
    private static final Set<String> LIQUID_INSTRUMENT_CODES = Set.of(
            "LIQUID_FUND"  // Liquid mutual funds — can be redeemed in 1 business day
    );

    /**
     * Account types considered "liquid" for emergency fund.
     * SAVINGS accounts hold easily accessible money (bank savings).
     */
    private static final Set<String> LIQUID_ACCOUNT_TYPES = Set.of(
            "SAVINGS"
    );

    public NetWorthService(HoldingRepository holdingRepository,
                           LiabilityRepository liabilityRepository,
                           NetWorthSnapshotRepository snapshotRepository,
                           HouseholdRepository householdRepository,
                           CashFlowService cashFlowService,
                           ArthaProperties arthaProperties,
                           ObjectMapper objectMapper) {
        this.holdingRepository = holdingRepository;
        this.liabilityRepository = liabilityRepository;
        this.snapshotRepository = snapshotRepository;
        this.householdRepository = householdRepository;
        this.cashFlowService = cashFlowService;
        this.arthaProperties = arthaProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Compute current net worth for a household (live, not from snapshot).
     *
     * Uses Algorithm A-9: O(h + l) where h = holdings, l = liabilities.
     */
    public NetWorthResponse computeCurrentNetWorth(UUID householdId) {
        validateHousehold(householdId);

        NetWorthResult result = computeNetWorthResult(householdId);

        // Look up previous snapshots for MoM/YoY comparison
        Optional<NetWorthSnapshot> lastSnapshot =
                snapshotRepository.findTopByHouseholdIdOrderBySnapshotDateDesc(householdId);

        BigDecimal momChange = lastSnapshot
                .map(s -> result.netWorth().subtract(s.getNetWorth()))
                .orElse(null);

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        BigDecimal yoyChange = snapshotRepository
                .findByHouseholdIdAndSnapshotDate(householdId, oneYearAgo)
                .map(s -> result.netWorth().subtract(s.getNetWorth()))
                .orElse(null);

        return new NetWorthResponse(
                householdId,
                LocalDate.now(),
                result.totalAssets(),
                result.totalLiabilities(),
                result.netWorth(),
                result.assetsByClass(),
                momChange,
                yoyChange
        );
    }

    /**
     * Take a snapshot of the current net worth and persist it.
     *
     * This is the "append-only" operation per FR-5.3.
     * If a snapshot for today already exists, it's replaced (upsert by date).
     */
    @Transactional
    public NetWorthResponse takeSnapshot(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        NetWorthResult result = computeNetWorthResult(householdId);
        LocalDate today = LocalDate.now();

        // Check for existing snapshot today (unique constraint handles race condition)
        Optional<NetWorthSnapshot> existingToday =
                snapshotRepository.findByHouseholdIdAndSnapshotDate(householdId, today);

        // Use a final reference so lambdas below can capture it safely
        final NetWorthSnapshot snapshot = existingToday.orElseGet(NetWorthSnapshot::new);
        snapshot.setHousehold(household);
        snapshot.setSnapshotDate(today);
        snapshot.setTotalAssets(result.totalAssets());
        snapshot.setTotalLiabilities(result.totalLiabilities());
        snapshot.setNetWorth(result.netWorth());
        snapshot.setAssetsByClassJson(serializeMap(result.assetsByClass()));

        // MoM: compare with previous month's snapshot
        LocalDate lastMonth = today.minusMonths(1);
        snapshotRepository.findByHouseholdIdAndSnapshotDate(householdId, lastMonth)
                .ifPresent(prev -> snapshot.setMonthOverMonthChange(
                        result.netWorth().subtract(prev.getNetWorth())));

        // YoY: compare with same month last year's snapshot
        LocalDate lastYear = today.minusYears(1);
        snapshotRepository.findByHouseholdIdAndSnapshotDate(householdId, lastYear)
                .ifPresent(prev -> snapshot.setYearOverYearChange(
                        result.netWorth().subtract(prev.getNetWorth())));

        NetWorthSnapshot saved = snapshotRepository.save(snapshot);

        return new NetWorthResponse(
                householdId,
                saved.getSnapshotDate(),
                saved.getTotalAssets(),
                saved.getTotalLiabilities(),
                saved.getNetWorth(),
                result.assetsByClass(),
                saved.getMonthOverMonthChange(),
                saved.getYearOverYearChange()
        );
    }

    /**
     * Get snapshot history for a household (most recent first).
     */
    public List<NetWorthResponse> getHistory(UUID householdId) {
        validateHousehold(householdId);
        return snapshotRepository.findByHouseholdIdOrderBySnapshotDateDesc(householdId)
                .stream()
                .map(this::snapshotToResponse)
                .toList();
    }

    /**
     * Compute emergency fund status for a household.
     *
     * SPEC REF: FR-5.2
     * Uses Algorithm A-10: filter + sum, O(h).
     *
     * @return EmergencyFundStatus with months covered, shortfall, and adequacy.
     */
    public EmergencyFundStatus computeEmergencyFund(UUID householdId) {
        validateHousehold(householdId);

        // ─── Step 1: Get monthly essential (consumption) expenses ───
        // We use the cash-flow engine's existing computation
        BigDecimal monthlyEssentialExpenses = cashFlowService
                .computeMonthlyCashFlow(householdId)
                .consumptionExpenses();

        // ─── Step 2: Sum liquid reserves ───
        // Per FR-5.2: "savings accounts + liquid MF + FD maturing within 3 months"
        List<Holding> activeHoldings = holdingRepository.findByOwnerHouseholdIdAndStatus(
                householdId, HoldingStatus.ACTIVE);

        BigDecimal liquidReserve = activeHoldings.stream()
                .filter(this::isLiquid)
                .map(Holding::getCurrentValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Step 3: Delegate to the pure calculator ───
        int targetMonths = arthaProperties.getEmergencyFundMonths();
        return EmergencyFundCalculator.compute(monthlyEssentialExpenses, liquidReserve, targetMonths);
    }

    // ─── Private helpers ─────────────────────────────────────

    private NetWorthResult computeNetWorthResult(UUID householdId) {
        // Collect active holdings as engine input
        List<HoldingValue> holdingValues = holdingRepository
                .findByOwnerHouseholdIdAndStatus(householdId, HoldingStatus.ACTIVE)
                .stream()
                .map(h -> new HoldingValue(
                        h.getCurrentValue(),
                        h.getInstrumentType() != null ? h.getInstrumentType().getAssetClass() : "OTHER"
                ))
                .toList();

        // Collect active liabilities as engine input
        List<LiabilityValue> liabilityValues = liabilityRepository
                .findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .map(l -> new LiabilityValue(l.getPrincipalOutstanding()))
                .toList();

        return NetWorthEngine.compute(holdingValues, liabilityValues);
    }

    /**
     * Check if a holding is "liquid" for emergency fund purposes.
     *
     * A holding is liquid if:
     *   1. Its account type is SAVINGS, OR
     *   2. Its instrument type code is in the LIQUID_INSTRUMENT_CODES set
     *
     * Future: also check FDs with maturity within EMERGENCY_LIQUID_MATURITY_DAYS (90 days).
     * This requires a maturityDate field on Holding, which we'll add when needed.
     */
    private boolean isLiquid(Holding holding) {
        // Check account type
        if (holding.getAccount() != null
                && LIQUID_ACCOUNT_TYPES.contains(holding.getAccount().getAccountType().name())) {
            return true;
        }
        // Check instrument type code
        if (holding.getInstrumentType() != null
                && LIQUID_INSTRUMENT_CODES.contains(holding.getInstrumentType().getCode())) {
            return true;
        }
        return false;
    }

    private void validateHousehold(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new RuntimeException("Household not found: " + householdId);
        }
    }

    private String serializeMap(Map<String, BigDecimal> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private NetWorthResponse snapshotToResponse(NetWorthSnapshot snapshot) {
        Map<String, BigDecimal> assetsByClass = Map.of();
        if (snapshot.getAssetsByClassJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> raw = objectMapper.readValue(
                        snapshot.getAssetsByClassJson(), Map.class);
                assetsByClass = raw.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> new BigDecimal(e.getValue().toString())
                        ));
            } catch (JsonProcessingException e) {
                // Fall through with empty map
            }
        }
        return new NetWorthResponse(
                snapshot.getHousehold().getId(),
                snapshot.getSnapshotDate(),
                snapshot.getTotalAssets(),
                snapshot.getTotalLiabilities(),
                snapshot.getNetWorth(),
                assetsByClass,
                snapshot.getMonthOverMonthChange(),
                snapshot.getYearOverYearChange()
        );
    }
}
