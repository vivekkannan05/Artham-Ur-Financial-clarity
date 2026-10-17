package com.artha.service;

import com.artha.domain.Goal;
import com.artha.domain.GoalStatus;
import com.artha.dto.GoalProjection;
import com.artha.dto.ScenarioRequest;
import com.artha.dto.ScenarioResult;
import com.artha.dto.ScenarioResult.GoalScenarioImpact;
import com.artha.engine.FundingProjectionEngine;
import com.artha.engine.NetWorthEngine;
import com.artha.engine.NetWorthEngine.HoldingValue;
import com.artha.engine.NetWorthEngine.LiabilityValue;
import com.artha.repository.GoalRepository;
import com.artha.repository.HoldingRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.LiabilityRepository;
import com.artha.domain.HoldingStatus;
import com.artha.domain.LiabilityStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scenario Analysis Service — What-if modeling (FR-6.4).
 *
 * SPEC REF: FR-6.4 — "User can modify variables and see impact on goal funding,
 * corpus projections, cash flow, and net worth. Scenario results are computed
 * on-the-fly and do NOT modify stored data."
 *
 * EC-13: "Extreme values (e.g., -50% crash + 20% inflation) → calculate and
 * display even if negative net worth. Add caveat."
 *
 * HOW IT WORKS:
 *   1. Take current financial state as baseline
 *   2. Apply user's variable overrides (inflationRate, expectedReturn, etc.)
 *   3. Re-run projection engines with overridden values
 *   4. Return deltas: what changed vs baseline
 *
 * IMPORTANT: This service NEVER writes to the database. All computation is
 * in-memory using the existing engines. The overrides are temporary.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private final GoalRepository goalRepository;
    private final GoalService goalService;
    private final HoldingRepository holdingRepository;
    private final LiabilityRepository liabilityRepository;
    private final HouseholdRepository householdRepository;
    private final CashFlowService cashFlowService;

    public ScenarioService(GoalRepository goalRepository,
                           GoalService goalService,
                           HoldingRepository holdingRepository,
                           LiabilityRepository liabilityRepository,
                           HouseholdRepository householdRepository,
                           CashFlowService cashFlowService) {
        this.goalRepository = goalRepository;
        this.goalService = goalService;
        this.holdingRepository = holdingRepository;
        this.liabilityRepository = liabilityRepository;
        this.householdRepository = householdRepository;
        this.cashFlowService = cashFlowService;
    }

    /**
     * Run a what-if scenario for a household.
     *
     * Supported override keys:
     *   - inflationRate: override goal inflation rates
     *   - expectedReturn: override goal expected return rates
     *   - marketCrashPct: apply immediate % drop to equity holdings
     *   - sipAmount: override all SIP amounts (future: per-goal)
     */
    public ScenarioResult analyze(UUID householdId, ScenarioRequest request) {
        if (!householdRepository.existsById(householdId)) {
            throw new RuntimeException("Household not found: " + householdId);
        }

        Map<String, BigDecimal> overrides = request.overrides();

        // ─── Baseline: current state ───
        var currentCashFlow = cashFlowService.computeMonthlyCashFlow(householdId);
        BigDecimal currentFreeCashFlow = currentCashFlow.freeCashFlow();

        var holdingValues = holdingRepository
                .findByOwnerHouseholdIdAndStatus(householdId, HoldingStatus.ACTIVE)
                .stream()
                .map(h -> new HoldingValue(h.getCurrentValue(),
                        h.getInstrumentType() != null ? h.getInstrumentType().getAssetClass() : "OTHER"))
                .toList();
        var liabilityValues = liabilityRepository
                .findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .map(l -> new LiabilityValue(l.getPrincipalOutstanding()))
                .toList();

        var baselineNW = NetWorthEngine.compute(holdingValues, liabilityValues);

        // ─── Scenario: apply overrides ───
        BigDecimal marketCrashPct = overrides.getOrDefault("marketCrashPct", BigDecimal.ZERO);
        boolean isExtremeScenario = marketCrashPct.abs().compareTo(new BigDecimal("30")) > 0;

        // Apply market crash to equity holdings
        List<HoldingValue> scenarioHoldings = holdingValues.stream()
                .map(h -> {
                    if ("EQUITY".equals(h.assetClass()) && h.currentValue() != null
                            && marketCrashPct.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal factor = BigDecimal.ONE.subtract(
                                marketCrashPct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                        return new HoldingValue(
                                h.currentValue().multiply(factor).setScale(2, RoundingMode.HALF_UP),
                                h.assetClass());
                    }
                    return h;
                })
                .toList();

        var scenarioNW = NetWorthEngine.compute(scenarioHoldings, liabilityValues);

        // ─── Goal impacts ───
        BigDecimal inflationOverride = overrides.get("inflationRate");
        BigDecimal returnOverride = overrides.get("expectedReturn");

        List<GoalScenarioImpact> goalImpacts = new ArrayList<>();
        List<Goal> activeGoals = goalRepository.findByHouseholdIdAndStatus(householdId, GoalStatus.ACTIVE);

        for (Goal goal : activeGoals) {
            // Original projection
            GoalProjection original = goalService.computeProjection(goal.getId());

            // Scenario projection with overridden rates
            BigDecimal inflation = inflationOverride != null
                    ? inflationOverride.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    : goal.getInflationRate();
            BigDecimal returnRate = returnOverride != null
                    ? returnOverride.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    : goal.getExpectedReturnRate();

            long months = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
            int years = Math.max(1, (int) (months / 12));

            BigDecimal scenarioTarget = FundingProjectionEngine.inflationAdjustedTarget(
                    goal.getTargetAmountToday(), inflation, years);
            BigDecimal scenarioCorpus = FundingProjectionEngine.corpusFutureValue(
                    original.currentCorpus(), returnRate, years);
            BigDecimal scenarioFundingPct = scenarioTarget.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.valueOf(100)
                    : scenarioCorpus.divide(scenarioTarget, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal scenarioShortfall = scenarioTarget.subtract(scenarioCorpus)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            goalImpacts.add(new GoalScenarioImpact(
                    goal.getName(),
                    original.fundingPercentage(),
                    scenarioFundingPct,
                    scenarioFundingPct.subtract(original.fundingPercentage()),
                    original.shortfallOrSurplus().max(BigDecimal.ZERO),
                    scenarioShortfall
            ));
        }

        // ─── Build result ───
        String caveat = isExtremeScenario
                ? "This is an extreme scenario. Results are for planning purposes only."
                : null;

        return new ScenarioResult(
                overrides,
                goalImpacts,
                scenarioNW.netWorth(),
                currentFreeCashFlow,  // Cash flow doesn't change instantly from market crash
                scenarioNW.netWorth().subtract(baselineNW.netWorth()),
                BigDecimal.ZERO,
                isExtremeScenario,
                caveat
        );
    }
}
