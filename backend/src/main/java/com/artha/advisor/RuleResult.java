package com.artha.advisor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * RuleResult — Output of a single rule evaluation.
 *
 * SPEC REF: FR-7.1 — Rules produce "flags/recommendations with severity and category."
 * Each rule can produce multiple results (e.g., one per goal that's underfunded).
 */
public record RuleResult(
        String ruleName,
        int priority,
        String severity,         // CRITICAL, WARNING, INFO
        String category,         // e.g., "Goal Adequacy", "Emergency Fund"
        String trigger,          // What condition fired this rule
        Map<String, Object> evidence,  // Numeric facts that support the flag
        String action,           // What the user should do
        String expectedImpact,   // What happens if they act
        String downsideRisk      // What happens if they don't
) {}
