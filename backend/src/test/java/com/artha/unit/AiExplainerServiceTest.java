package com.artha.unit;

import com.artha.advisor.RuleResult;
import com.artha.advisor.ai.AiExplainerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AiExplainerService — Layer 3 of the advisor pipeline.
 * SPEC REF: FR-7.1 (Layer 3), NFR-5.2 (AI doesn't contradict calcs)
 */
@DisplayName("AiExplainerService — Rule-Text Explanations")
class AiExplainerServiceTest {

    private AiExplainerService service;

    @BeforeEach
    void setUp() {
        service = new AiExplainerService();
    }

    @Test
    @DisplayName("Empty results → 'on track' message")
    void testEmptyResults() {
        String explanation = service.explain(List.of());
        assertTrue(explanation.contains("on track"));
    }

    @Test
    @DisplayName("Null results → 'on track' message")
    void testNullResults() {
        String explanation = service.explain(null);
        assertTrue(explanation.contains("on track"));
    }

    @Test
    @DisplayName("Single result → structured explanation with severity, action, impact")
    void testSingleResult() {
        RuleResult result = new RuleResult(
                "GoalAdequacyRule", 1, "CRITICAL", "Goal Adequacy",
                "Retirement is only 50% funded",
                Map.of("fundingPct", 50),
                "Increase SIP by ₹10,000/month",
                "Goal reaches target on time",
                "Shortfall may grow with inflation");

        String explanation = service.explain(List.of(result));
        assertTrue(explanation.contains("1 item"));
        assertTrue(explanation.contains("[CRITICAL]"));
        assertTrue(explanation.contains("Increase SIP"));
        assertTrue(explanation.contains("1 critical"));
    }

    @Test
    @DisplayName("Multiple results → correct severity summary")
    void testMultipleResults() {
        RuleResult critical = new RuleResult("R1", 1, "CRITICAL", "C1",
                "Trigger1", Map.of(), "Act1", "Impact1", "Risk1");
        RuleResult warning = new RuleResult("R2", 2, "WARNING", "C2",
                "Trigger2", Map.of(), "Act2", "Impact2", "Risk2");
        RuleResult info = new RuleResult("R3", 3, "INFO", "C3",
                "Trigger3", Map.of(), "Act3", "Impact3", "Risk3");

        String explanation = service.explain(List.of(critical, warning, info));
        assertTrue(explanation.contains("3 item"));
        assertTrue(explanation.contains("1 critical"));
        assertTrue(explanation.contains("1 warning"));
        assertTrue(explanation.contains("1 informational"));
    }

    @Test
    @DisplayName("explainSingle produces a complete sentence")
    void testExplainSingle() {
        RuleResult result = new RuleResult(
                "EmergencyFundRule", 2, "CRITICAL", "Emergency Fund",
                "Only 1 month covered",
                Map.of(), "Build up to 6 months",
                "Financial security", "Vulnerability to job loss");

        String explanation = service.explainSingle(result);
        assertTrue(explanation.contains("Emergency Fund"));
        assertTrue(explanation.contains("Only 1 month"));
        assertTrue(explanation.contains("Build up to 6 months"));
    }
}
