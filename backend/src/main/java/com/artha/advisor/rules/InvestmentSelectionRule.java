package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Investment Selection Rule — Priority 7.
 *
 * SPEC REF: FR-7.1 priority order position 7 — "Investment Selection."
 *
 * WHAT IT CHECKS:
 *   Detects if the household has too few distinct asset classes.
 *   A healthy portfolio should have at least 3 asset classes
 *   (e.g., equity, debt, gold / real estate).
 *
 * WHY THIS MATTERS:
 *   Households that only invest in one or two asset classes miss the
 *   diversification benefit even if no single class is over-concentrated.
 *   Example: 50% equity + 50% debt is fine, but adding 10% gold
 *   would improve risk-adjusted returns.
 *
 * PHASE 1 LIMITATION:
 *   This rule uses asset class count as a proxy. Phase 2 could evaluate
 *   individual instrument quality (expense ratios, tracking error, etc.).
 */
@Component
public class InvestmentSelectionRule implements Rule {

    private static final int MIN_ASSET_CLASSES = 3;

    @Override public int priority() { return 7; }
    @Override public String category() { return "Investment Selection"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.assetsByClass() != null
                && !context.assetsByClass().isEmpty()
                && context.totalAssets().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        int classCount = context.assetsByClass().size();

        if (classCount < MIN_ASSET_CLASSES) {
            return List.of(new RuleResult(
                    "InvestmentSelectionRule", priority(), "INFO", category(),
                    String.format("Portfolio has only %d asset class(es) — consider diversifying", classCount),
                    Map.of("assetClassCount", classCount,
                            "assetClasses", String.join(", ", context.assetsByClass().keySet())),
                    "Add investments in at least " + MIN_ASSET_CLASSES
                            + " asset classes (e.g., equity, debt, gold/alternatives)",
                    "Better risk-adjusted returns through multi-asset diversification",
                    "Limited asset classes increase correlation risk during market downturns"
            ));
        }
        return List.of();
    }
}
