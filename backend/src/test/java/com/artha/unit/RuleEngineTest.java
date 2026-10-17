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

    // ─── Context builder helper ──────────────────────────────

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
                gaps, dsr);
    }
}
