package com.artha.advisor;

import java.util.List;

/**
 * Rule — Interface for all financial health rules.
 *
 * SPEC REF: FR-7.1 — "Java rule engine evaluates conditions against facts."
 * Design Doc AD-1 — Rule interface with priority-ordered evaluation.
 *
 * DESIGN PATTERN:
 *   Strategy Pattern — each rule implementation encapsulates one financial
 *   health check. Rules are auto-discovered as Spring beans and sorted by
 *   priority. The AdvisorService iterates them in order.
 *
 * PRIORITY ORDER (from Spec Sec 14.2):
 *   1. Goal Adequacy
 *   2. Emergency Fund
 *   3. Debt Management
 *   4. Insurance Gaps
 *   5. Asset Allocation
 *   6. Contribution Adequacy
 *   7. Investment Selection
 *   8. Tax Optimization
 */
public interface Rule {

    /** Priority rank (1 = highest). Used for evaluation order. */
    int priority();

    /** Category name for display (e.g., "Goal Adequacy"). */
    String category();

    /** Whether this rule is applicable given the current context. */
    boolean appliesTo(HouseholdFinancialContext context);

    /** Evaluate the rule and return zero or more results. */
    List<RuleResult> evaluate(HouseholdFinancialContext context);
}
