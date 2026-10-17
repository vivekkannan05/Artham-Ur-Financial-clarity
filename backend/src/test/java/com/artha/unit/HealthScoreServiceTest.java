package com.artha.unit;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.domain.EmergencyFundAdequacy;
import com.artha.dto.*;
import com.artha.dto.HealthScore.SubScore;
import com.artha.service.HealthScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-16: Financial health sub-scores.
 * SPEC REF: FR-7.3 — "Fixture with known values across all 7 dimensions."
 */
@DisplayName("UT-16: Financial Health Sub-Scores")
class HealthScoreServiceTest {

    private HealthScoreService service;

    @BeforeEach
    void setUp() {
        service = new HealthScoreService();
    }

    @Test
    @DisplayName("UT-16: All 7 sub-scores computed with fixture data")
    void testAllSubScores() {
        HouseholdFinancialContext context = buildFixtureContext(
                new BigDecimal("25.000000"),   // savings rate 25%
                new BigDecimal("15.000000"),   // DSR 15%
                new BigDecimal("8.00"),        // 8 months emergency
                new BigDecimal("85.00"),       // 85% goal funding
                Map.of("EQUITY", new BigDecimal("600000"),
                       "DEBT", new BigDecimal("400000")),  // 60/40 split
                0                               // no insurance gaps
        );

        HealthScore score = service.compute(context);

        assertEquals(7, score.subScores().size());

        SubScore cashFlow = findByCategory(score, "Cash-Flow Health");
        assertEquals(new BigDecimal("100"), cashFlow.score());
        assertEquals("Green", cashFlow.status());

        SubScore debt = findByCategory(score, "Debt Health");
        assertEquals(new BigDecimal("80"), debt.score());
        assertEquals("Green", debt.status());

        SubScore emergency = findByCategory(score, "Emergency Readiness");
        assertEquals(new BigDecimal("100"), emergency.score());

        SubScore goals = findByCategory(score, "Goal Progress");
        assertEquals(new BigDecimal("85"), goals.score());

        SubScore diversification = findByCategory(score, "Diversification");
        assertEquals(new BigDecimal("60"), diversification.score());  // 60% in equity > 50%

        SubScore insurance = findByCategory(score, "Insurance Adequacy");
        assertEquals(new BigDecimal("100"), insurance.score());

        assertTrue(score.overallScore().compareTo(new BigDecimal("70")) >= 0,
                "Overall score should be Green");
    }

    @Test
    @DisplayName("UT-16: Poor health — negative cash flow, high debt, no emergency fund")
    void testPoorHealth() {
        HouseholdFinancialContext context = buildFixtureContext(
                new BigDecimal("-5.000000"),   // negative savings
                new BigDecimal("45.000000"),   // DSR 45% (above 40%)
                new BigDecimal("1.00"),        // 1 month emergency
                new BigDecimal("30.00"),       // 30% goal funding
                Map.of("EQUITY", new BigDecimal("900000"),
                       "DEBT", new BigDecimal("100000")),  // 90/10 concentrated
                2                               // 2 insurance gaps
        );

        HealthScore score = service.compute(context);

        SubScore cashFlow = findByCategory(score, "Cash-Flow Health");
        assertEquals(BigDecimal.ZERO, cashFlow.score());
        assertEquals("Red", cashFlow.status());

        SubScore debt = findByCategory(score, "Debt Health");
        assertEquals(BigDecimal.TEN, debt.score());
        assertEquals("Red", debt.status());

        SubScore emergency = findByCategory(score, "Emergency Readiness");
        assertEquals(new BigDecimal("20"), emergency.score());

        assertTrue(score.overallScore().compareTo(new BigDecimal("40")) < 0,
                "Overall should be Red for poor health");
    }

    @Test
    @DisplayName("Status thresholds: Green >= 70, Yellow >= 40, Red < 40")
    void testStatusThresholds() {
        assertEquals("Green", HealthScoreService.statusFromScore(new BigDecimal("100")));
        assertEquals("Green", HealthScoreService.statusFromScore(new BigDecimal("70")));
        assertEquals("Yellow", HealthScoreService.statusFromScore(new BigDecimal("69")));
        assertEquals("Yellow", HealthScoreService.statusFromScore(new BigDecimal("40")));
        assertEquals("Red", HealthScoreService.statusFromScore(new BigDecimal("39")));
        assertEquals("Red", HealthScoreService.statusFromScore(BigDecimal.ZERO));
    }

    // ─── Fixture builders ────────────────────────────────────

    private HouseholdFinancialContext buildFixtureContext(
            BigDecimal savingsRate, BigDecimal dsr, BigDecimal emergencyMonths,
            BigDecimal goalFundingPct, Map<String, BigDecimal> assetsByClass,
            int insuranceGapCount) {

        CashFlowStatement cashFlow = new CashFlowStatement(
                UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("80000"),
                new BigDecimal("70000"), new BigDecimal("150000"),
                new BigDecimal("50000"), savingsRate, dsr);

        EmergencyFundStatus ef = new EmergencyFundStatus(
                new BigDecimal("80000"), new BigDecimal("480000"),
                emergencyMonths.multiply(new BigDecimal("80000")),
                emergencyMonths, BigDecimal.ZERO,
                emergencyMonths.compareTo(new BigDecimal("6")) >= 0
                        ? EmergencyFundAdequacy.ADEQUATE
                        : EmergencyFundAdequacy.CRITICAL);

        GoalProjection gp = new GoalProjection(
                new BigDecimal("5000000"), new BigDecimal("8000000"), 10,
                new BigDecimal("2000000"), new BigDecimal("6800000"),
                goalFundingPct, new BigDecimal("1200000"),
                new BigDecimal("15000"), new BigDecimal("12000"));

        BigDecimal totalAssets = assetsByClass.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InsuranceCoverageGap> gaps = new java.util.ArrayList<>();
        for (int i = 0; i < insuranceGapCount; i++) {
            gaps.add(new InsuranceCoverageGap("TERM_LIFE", "Person",
                    new BigDecimal("10000000"), new BigDecimal("5000000"),
                    new BigDecimal("5000000"), "Gap " + (i + 1)));
        }

        return new HouseholdFinancialContext(
                UUID.randomUUID(), "Test Household", 2,
                cashFlow, List.of(gp), List.of("Retirement"),
                ef, totalAssets, BigDecimal.ZERO, totalAssets,
                assetsByClass, gaps, dsr);
    }

    private SubScore findByCategory(HealthScore score, String category) {
        return score.subScores().stream()
                .filter(s -> s.category().equals(category))
                .findFirst()
                .orElseThrow(() -> new AssertionError("SubScore not found: " + category));
    }
}
