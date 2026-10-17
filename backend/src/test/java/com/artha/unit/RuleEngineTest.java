package com.artha.unit;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.RuleResult;
import com.artha.advisor.rules.*;
import com.artha.domain.EmergencyFundAdequacy;
import com.artha.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-20: Rule engine — each rule fires correctly.
 * SPEC REF: FR-7.1 — "Fixture per rule: emergency low, equity high, insurance gap, etc."
 */
@DisplayName("UT-20: Rule Engine — Individual Rules")
class RuleEngineTest {

    @Test
    @DisplayName("GoalAdequacyRule: fires when funding < 80%")
    void testGoalAdequacyFires() {
        GoalProjection underfunded = new GoalProjection(
                new BigDecimal("5000000"), new BigDecimal("8000000"), 10,
                new BigDecimal("1000000"), new BigDecimal("4000000"),
                new BigDecimal("50.00"), new BigDecimal("4000000"),
                new BigDecimal("20000"), new BigDecimal("18000"));

        var context = contextWith(List.of(underfunded), List.of("Retirement"),
                null, BigDecimal.ZERO, List.of());

        GoalAdequacyRule rule = new GoalAdequacyRule();
        assertTrue(rule.appliesTo(context));
        List<RuleResult> results = rule.evaluate(context);

        assertEquals(1, results.size());
        assertEquals("CRITICAL", results.get(0).severity());
        assertTrue(results.get(0).trigger().contains("50.0%"));
    }

    @Test
    @DisplayName("GoalAdequacyRule: does NOT fire when funding >= 80%")
    void testGoalAdequacyDoesNotFire() {
        GoalProjection funded = new GoalProjection(
                new BigDecimal("5000000"), new BigDecimal("8000000"), 10,
                new BigDecimal("6500000"), new BigDecimal("7500000"),
                new BigDecimal("90.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        var context = contextWith(List.of(funded), List.of("Retirement"),
                null, BigDecimal.ZERO, List.of());

        List<RuleResult> results = new GoalAdequacyRule().evaluate(context);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("EmergencyFundRule: fires when fund is critical")
    void testEmergencyFundFires() {
        EmergencyFundStatus critical = new EmergencyFundStatus(
                new BigDecimal("60000"), new BigDecimal("360000"),
                new BigDecimal("60000"), new BigDecimal("1.00"),
                new BigDecimal("300000"), EmergencyFundAdequacy.CRITICAL);

        var context = contextWith(List.of(), List.of(), critical, BigDecimal.ZERO, List.of());

        EmergencyFundRule rule = new EmergencyFundRule();
        assertTrue(rule.appliesTo(context));
        List<RuleResult> results = rule.evaluate(context);

        assertEquals(1, results.size());
        assertEquals("CRITICAL", results.get(0).severity());
    }

    @Test
    @DisplayName("EmergencyFundRule: does NOT fire when adequate")
    void testEmergencyFundDoesNotFire() {
        EmergencyFundStatus adequate = new EmergencyFundStatus(
                new BigDecimal("60000"), new BigDecimal("360000"),
                new BigDecimal("480000"), new BigDecimal("8.00"),
                BigDecimal.ZERO, EmergencyFundAdequacy.ADEQUATE);

        var context = contextWith(List.of(), List.of(), adequate, BigDecimal.ZERO, List.of());
        assertTrue(new EmergencyFundRule().evaluate(context).isEmpty());
    }

    @Test
    @DisplayName("DebtManagementRule: fires when DSR > 40%")
    void testDebtManagementFires() {
        var context = contextWith(List.of(), List.of(), null,
                new BigDecimal("45.000000"), List.of());

        DebtManagementRule rule = new DebtManagementRule();
        assertTrue(rule.appliesTo(context));
        List<RuleResult> results = rule.evaluate(context);

        assertEquals(1, results.size());
        assertEquals("CRITICAL", results.get(0).severity());
        assertTrue(results.get(0).trigger().contains("45.0%"));
    }

    @Test
    @DisplayName("DebtManagementRule: does NOT fire when DSR <= 40%")
    void testDebtManagementDoesNotFire() {
        var context = contextWith(List.of(), List.of(), null,
                new BigDecimal("20.000000"), List.of());
        assertTrue(new DebtManagementRule().evaluate(context).isEmpty());
    }

    @Test
    @DisplayName("InsuranceGapRule: fires with gaps, silent without")
    void testInsuranceGapRule() {
        InsuranceCoverageGap gap = new InsuranceCoverageGap(
                "TERM_LIFE", "Rahul",
                new BigDecimal("10000000"), new BigDecimal("5000000"),
                new BigDecimal("5000000"),
                "Term life cover is ₹50L — recommended ₹1Cr");

        var contextWithGap = contextWith(List.of(), List.of(), null,
                BigDecimal.ZERO, List.of(gap));
        var contextNoGap = contextWith(List.of(), List.of(), null,
                BigDecimal.ZERO, List.of());

        InsuranceGapRule rule = new InsuranceGapRule();
        assertTrue(rule.appliesTo(contextWithGap));
        assertEquals(1, rule.evaluate(contextWithGap).size());

        assertFalse(rule.appliesTo(contextNoGap));
    }

    // ─── New rules: Allocation, Contribution, InvestmentSelection, TaxOptimization ───

    @Test
    @DisplayName("AllocationRule: fires when single asset class > 85%")
    void testAllocationCritical() {
        var context = contextWithAssets(
                Map.of("EQUITY", new BigDecimal("900000"), "DEBT", new BigDecimal("100000")));

        AllocationRule rule = new AllocationRule();
        assertTrue(rule.appliesTo(context));
        List<RuleResult> results = rule.evaluate(context);
        assertEquals(1, results.size());
        assertEquals("CRITICAL", results.get(0).severity());
        assertTrue(results.get(0).trigger().contains("EQUITY"));
    }

    @Test
    @DisplayName("AllocationRule: warns when single asset class 70-85%")
    void testAllocationWarning() {
        var context = contextWithAssets(
                Map.of("EQUITY", new BigDecimal("750000"), "DEBT", new BigDecimal("250000")));

        List<RuleResult> results = new AllocationRule().evaluate(context);
        assertEquals(1, results.size());
        assertEquals("WARNING", results.get(0).severity());
    }

    @Test
    @DisplayName("AllocationRule: silent when well-diversified")
    void testAllocationSilent() {
        var context = contextWithAssets(
                Map.of("EQUITY", new BigDecimal("500000"), "DEBT", new BigDecimal("500000")));
        assertTrue(new AllocationRule().evaluate(context).isEmpty());
    }

    @Test
    @DisplayName("ContributionRule: fires when goal needs SIP")
    void testContributionFires() {
        GoalProjection needsSip = new GoalProjection(
                new BigDecimal("5000000"), new BigDecimal("8000000"), 10,
                new BigDecimal("2000000"), new BigDecimal("4000000"),
                new BigDecimal("50.00"), new BigDecimal("4000000"),
                new BigDecimal("20000"), new BigDecimal("18000"));

        var context = contextWith(List.of(needsSip), List.of("Education"),
                null, BigDecimal.ZERO, List.of());

        ContributionRule rule = new ContributionRule();
        List<RuleResult> results = rule.evaluate(context);
        assertEquals(1, results.size());
        assertEquals("INFO", results.get(0).severity());
        assertTrue(results.get(0).trigger().contains("Education"));
    }

    @Test
    @DisplayName("ContributionRule: silent when goal is fully funded")
    void testContributionSilentWhenFunded() {
        GoalProjection funded = new GoalProjection(
                new BigDecimal("5000000"), new BigDecimal("8000000"), 10,
                new BigDecimal("8000000"), new BigDecimal("8000000"),
                new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        var context = contextWith(List.of(funded), List.of("Retirement"),
                null, BigDecimal.ZERO, List.of());
        assertTrue(new ContributionRule().evaluate(context).isEmpty());
    }

    @Test
    @DisplayName("InvestmentSelectionRule: fires when < 3 asset classes")
    void testInvestmentSelectionFires() {
        var context = contextWithAssets(Map.of("EQUITY", new BigDecimal("1000000")));

        InvestmentSelectionRule rule = new InvestmentSelectionRule();
        List<RuleResult> results = rule.evaluate(context);
        assertEquals(1, results.size());
        assertEquals("INFO", results.get(0).severity());
    }

    @Test
    @DisplayName("InvestmentSelectionRule: silent when >= 3 asset classes")
    void testInvestmentSelectionSilent() {
        var context = contextWithAssets(Map.of(
                "EQUITY", new BigDecimal("400000"),
                "DEBT", new BigDecimal("400000"),
                "GOLD", new BigDecimal("200000")));
        assertTrue(new InvestmentSelectionRule().evaluate(context).isEmpty());
    }

    @Test
    @DisplayName("TaxOptimizationRule: flags tax benefit when insurance gaps exist")
    void testTaxOptimizationInsuranceGap() {
        InsuranceCoverageGap gap = new InsuranceCoverageGap(
                "TERM_LIFE", "Rahul",
                new BigDecimal("10000000"), new BigDecimal("5000000"),
                new BigDecimal("5000000"), "Gap");

        var context = contextWith(List.of(), List.of(), null, BigDecimal.ZERO, List.of(gap));
        TaxOptimizationRule rule = new TaxOptimizationRule();
        List<RuleResult> results = rule.evaluate(context);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).trigger().contains("Insurance gaps"));
    }

    // ─── Context builder helpers ─────────────────────────────

    private HouseholdFinancialContext contextWith(
            List<GoalProjection> goals, List<String> goalNames,
            EmergencyFundStatus ef, BigDecimal dsr,
            List<InsuranceCoverageGap> gaps) {

        CashFlowStatement cashFlow = new CashFlowStatement(
                UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("80000"),
                new BigDecimal("70000"), new BigDecimal("150000"),
                new BigDecimal("50000"), new BigDecimal("25.000000"), dsr);

        return new HouseholdFinancialContext(
                UUID.randomUUID(), "Test", 2, cashFlow,
                goals, goalNames, ef,
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("1000000"),
                Map.of("EQUITY", new BigDecimal("1000000")),
                gaps, dsr, 0);
    }

    private HouseholdFinancialContext contextWithAssets(Map<String, BigDecimal> assetsByClass) {
        BigDecimal total = assetsByClass.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowStatement cashFlow = new CashFlowStatement(
                UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("80000"),
                new BigDecimal("70000"), new BigDecimal("150000"),
                new BigDecimal("50000"), new BigDecimal("25.000000"), BigDecimal.ZERO);

        return new HouseholdFinancialContext(
                UUID.randomUUID(), "Test", 2, cashFlow,
                List.of(), List.of(), null,
                total, BigDecimal.ZERO, total,
                assetsByClass, List.of(), BigDecimal.ZERO, 0);
    }
}
