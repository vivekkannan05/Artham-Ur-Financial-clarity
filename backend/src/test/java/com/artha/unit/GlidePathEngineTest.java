package com.artha.unit;

import com.artha.engine.GlidePathEngine;
import com.artha.engine.GlidePathEngine.Allocation;
import com.artha.engine.GlidePathEngine.Milestone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-08: Glide-path date-based interpolation
 * UT-09: Glide-path age-based (same algorithm, different input)
 * UT-10: Glide-path past target date (EC-9)
 */
@DisplayName("UT-08/09/10: Glide-Path Engine")
class GlidePathEngineTest {

    /** Education goal schedule: 70% equity at 8yr → 50% at 4yr → 20% at 1yr */
    private final Milestone[] educationSchedule = {
            new Milestone(8, new BigDecimal("70.00"), new BigDecimal("30.00")),
            new Milestone(4, new BigDecimal("50.00"), new BigDecimal("50.00")),
            new Milestone(1, new BigDecimal("20.00"), new BigDecimal("80.00"))
    };

    @Test
    @DisplayName("UT-08: 6 years out → interpolate between 8yr and 4yr milestones")
    void testInterpolationMidRange() {
        // 6 years to goal → between milestone[0](8yr=70%) and milestone[1](4yr=50%)
        // fraction = (8-6)/(8-4) = 0.5 → equity = 70 - 0.5*(70-50) = 60%
        Allocation result = GlidePathEngine.interpolate(educationSchedule, 6.0);

        assertEquals(new BigDecimal("60.00"), result.equityPct());
        assertEquals(new BigDecimal("40.00"), result.safePct());
    }

    @Test
    @DisplayName("UT-08: At exact milestone → returns milestone value")
    void testExactMilestone() {
        Allocation result = GlidePathEngine.interpolate(educationSchedule, 4.0);
        // At exactly 4 years → should be upper of [4yr, 1yr] bracket, i.e., 50%
        assertEquals(new BigDecimal("50.00"), result.equityPct());
    }

    @Test
    @DisplayName("UT-08: Far from goal (10 years) → first milestone allocation")
    void testFarFromGoal() {
        Allocation result = GlidePathEngine.interpolate(educationSchedule, 10.0);
        assertEquals(new BigDecimal("70.00"), result.equityPct());
    }

    @Test
    @DisplayName("UT-09: Age-based works with same algorithm (different inputs)")
    void testAgeBasedGlidePath() {
        // Retirement schedule: age 30=80% equity, age 50=60%, age 60=30%
        Milestone[] retirementSchedule = {
                new Milestone(30, new BigDecimal("80.00"), new BigDecimal("20.00")),
                new Milestone(10, new BigDecimal("60.00"), new BigDecimal("40.00")),
                new Milestone(0, new BigDecimal("30.00"), new BigDecimal("70.00"))
        };
        // At 20 years to retirement (age ~40): interpolate 80%→60%
        Allocation result = GlidePathEngine.interpolate(retirementSchedule, 20.0);
        assertEquals(new BigDecimal("70.00"), result.equityPct());
    }

    @Test
    @DisplayName("UT-10: Past target date (EC-9) → terminal allocation")
    void testPastTargetDate() {
        // Years to goal = -1 (goal overdue per EC-9)
        Allocation result = GlidePathEngine.interpolate(educationSchedule, -1.0);
        // Should freeze at terminal (most conservative) allocation
        assertEquals(new BigDecimal("20.00"), result.equityPct());
        assertEquals(new BigDecimal("80.00"), result.safePct());
    }
}
