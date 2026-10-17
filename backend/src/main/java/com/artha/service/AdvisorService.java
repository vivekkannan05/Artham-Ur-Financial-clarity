package com.artha.service;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import com.artha.domain.*;
import com.artha.dto.*;
import com.artha.dto.InsuranceCoverageGap.CoverageAnalysis;
import com.artha.engine.NetWorthEngine;
import com.artha.engine.NetWorthEngine.HoldingValue;
import com.artha.engine.NetWorthEngine.LiabilityValue;
import com.artha.engine.NetWorthEngine.NetWorthResult;
import com.artha.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advisor Service — Orchestrates the 3-layer advisor pipeline.
 *
 * SPEC REF: FR-7.1 — "Layered advisor engine: Layer 1 (calculations) →
 * Layer 2 (rule engine) → Layer 3 (AI explanation)."
 *
 * PIPELINE:
 *   1. Build HouseholdFinancialContext (one DB round-trip)
 *   2. Evaluate all rules against context (sorted by priority)
 *   3. AI explanation (Phase 1: fallback to rule text — Ollama integration future)
 *   4. Store as Recommendation entities
 *
 * Design Doc Sec 9.2: "If all rules pass, say 'do nothing'."
 */
@Service
@Transactional(readOnly = true)
public class AdvisorService {

    private final List<Rule> rules;
    private final CashFlowService cashFlowService;
    private final GoalService goalService;
    private final NetWorthService netWorthService;
    private final InsuranceService insuranceService;
    private final HoldingRepository holdingRepository;
    private final LiabilityRepository liabilityRepository;
    private final HouseholdRepository householdRepository;
    private final GoalRepository goalRepository;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public AdvisorService(List<Rule> rules,
                          CashFlowService cashFlowService,
                          GoalService goalService,
                          NetWorthService netWorthService,
                          InsuranceService insuranceService,
                          HoldingRepository holdingRepository,
                          LiabilityRepository liabilityRepository,
                          HouseholdRepository householdRepository,
                          GoalRepository goalRepository,
                          RecommendationRepository recommendationRepository,
                          ObjectMapper objectMapper) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(Rule::priority))
                .toList();
        this.cashFlowService = cashFlowService;
        this.goalService = goalService;
        this.netWorthService = netWorthService;
        this.insuranceService = insuranceService;
        this.holdingRepository = holdingRepository;
        this.liabilityRepository = liabilityRepository;
        this.householdRepository = householdRepository;
        this.goalRepository = goalRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Run the full advisor pipeline for a household.
     *
     * @return List of RuleResults (recommendations), sorted by priority.
     */
    public List<RuleResult> advise(UUID householdId) {
        HouseholdFinancialContext context = buildContext(householdId);

        List<RuleResult> results = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.appliesTo(context)) {
                results.addAll(rule.evaluate(context));
            }
        }

        return results;
    }

    /**
     * Run advisor and persist results as Recommendation entities.
     */
    @Transactional
    public List<RuleResult> adviseAndStore(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        List<RuleResult> results = advise(householdId);

        for (RuleResult r : results) {
            Recommendation rec = new Recommendation();
            rec.setHousehold(household);
            rec.setTriggerDescription(r.trigger());
            rec.setRuleName(r.ruleName());
            rec.setSeverity(Severity.valueOf(r.severity()));
            rec.setCategory(r.category());
            rec.setEvidenceJson(serializeEvidence(r.evidence()));
            rec.setAction(r.action());
            rec.setExpectedImpact(r.expectedImpact());
            rec.setDownsideRisk(r.downsideRisk());
            rec.setSpecVersion("SPEC-v1.1");
            rec.setStatus(RecommendationStatus.PENDING);
            recommendationRepository.save(rec);
        }

        return results;
    }

    /**
     * Build the shared context object — one DB round-trip for everything.
     * Design Doc Sec 9.3.
     */
    public HouseholdFinancialContext buildContext(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        // Cash flow
        CashFlowStatement cashFlow = cashFlowService.computeMonthlyCashFlow(householdId);

        // Goals + projections
        List<Goal> activeGoals = goalRepository.findByHouseholdIdAndStatus(householdId, GoalStatus.ACTIVE);
        List<GoalProjection> projections = activeGoals.stream()
                .map(g -> goalService.computeProjection(g.getId()))
                .toList();
        List<String> goalNames = activeGoals.stream().map(Goal::getName).toList();

        // Emergency fund
        EmergencyFundStatus emergencyFund = netWorthService.computeEmergencyFund(householdId);

        // Net worth
        List<HoldingValue> holdingValues = holdingRepository
                .findByOwnerHouseholdIdAndStatus(householdId, HoldingStatus.ACTIVE)
                .stream()
                .map(h -> new HoldingValue(h.getCurrentValue(),
                        h.getInstrumentType() != null ? h.getInstrumentType().getAssetClass() : "OTHER"))
                .toList();
        List<LiabilityValue> liabilityValues = liabilityRepository
                .findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .map(l -> new LiabilityValue(l.getPrincipalOutstanding()))
                .toList();
        NetWorthResult nw = NetWorthEngine.compute(holdingValues, liabilityValues);

        // Insurance gaps
        CoverageAnalysis coverage = insuranceService.analyzeCoverageGaps(householdId);

        return new HouseholdFinancialContext(
                householdId,
                household.getName(),
                0,  // member count — lightweight, not critical for rules
                cashFlow,
                projections,
                goalNames,
                emergencyFund,
                nw.totalAssets(),
                nw.totalLiabilities(),
                nw.netWorth(),
                nw.assetsByClass(),
                coverage.gaps(),
                cashFlow.debtServiceRatio()
        );
    }

    private String serializeEvidence(Map<String, Object> evidence) {
        try { return objectMapper.writeValueAsString(evidence); }
        catch (JsonProcessingException e) { return "{}"; }
    }
}
