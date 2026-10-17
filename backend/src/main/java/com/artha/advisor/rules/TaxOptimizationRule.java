package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tax Optimization Rule — Priority 8.
 *
 * SPEC REF: FR-7.1 priority order position 8 — "Tax Optimization."
 *
 * WHAT IT CHECKS:
 *   Phase 1 detects basic tax inefficiencies:
 *   1. If insurance gaps exist, flags that term life premiums are tax-deductible
 *      under Section 80C/80D (so closing the gap has a double benefit).
 *   2. If savings rate is high but cash is parked without tax-efficient instruments,
 *      suggests tax-saving vehicles (ELSS, NPS, PPF).
 *
 * PHASE 2 EXPANSION:
 *   - Tax-loss harvesting opportunities (harvest losses to offset gains)
 *   - Capital gains tax planning (STCG vs LTCG optimization)
 *   - Section 80C limit utilization tracking
 */
@Component
public class TaxOptimizationRule implements Rule {

    private static final BigDecimal HIGH_SAVINGS_RATE = new BigDecimal("25");

    @Override public int priority() { return 8; }
    @Override public String category() { return "Tax Optimization"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.cashFlow() != null;
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        List<RuleResult> results = new ArrayList<>();

        // Check 1: Insurance gap + tax benefit opportunity
        if (context.insuranceGaps() != null && !context.insuranceGaps().isEmpty()) {
            results.add(new RuleResult(
                    "TaxOptimizationRule", priority(), "INFO", category(),
                    "Insurance gaps exist — closing them also provides tax benefits",
                    Map.of("gapCount", context.insuranceGaps().size()),
                    "Term life premiums are deductible under Sec 80C (up to ₹1.5L). "
                            + "Health premiums are deductible under Sec 80D (up to ₹25K/₹50K for seniors)",
                    "Reduced taxable income while improving insurance coverage",
                    "Missed tax deductions increase effective tax outflow"
            ));
        }

        // Check 2: High savings rate but potentially missing tax-saving instruments
        if (context.cashFlow() != null
                && context.cashFlow().savingsRate().compareTo(HIGH_SAVINGS_RATE) > 0
                && (context.assetsByClass() == null || !context.assetsByClass().containsKey("TAX_SAVING"))) {
            results.add(new RuleResult(
                    "TaxOptimizationRule", priority(), "INFO", category(),
                    String.format("Savings rate is %.0f%% — ensure tax-saving instruments are utilized",
                            context.cashFlow().savingsRate()),
                    Map.of("savingsRate", context.cashFlow().savingsRate()),
                    "Consider ELSS (Sec 80C), NPS (Sec 80CCD), PPF, or tax-saving FDs",
                    "Potential annual tax savings of ₹46,800 at 30% slab (₹1.5L × 0.312)",
                    "Unused 80C limit means paying more tax than necessary"
            ));
        }

        return results;
    }
}
