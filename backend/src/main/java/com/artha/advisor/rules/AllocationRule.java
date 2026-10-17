package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Asset Allocation Rule — Priority 5.
 *
 * SPEC REF: FR-7.1 priority order position 5 — "Asset Allocation."
 *
 * WHAT IT CHECKS:
 *   Examines the household's asset class concentration. If any single
 *   asset class exceeds 70% of total assets, it fires a WARNING.
 *   If it exceeds 85%, it escalates to CRITICAL.
 *
 * WHY THIS MATTERS:
 *   Over-concentration in one asset class (e.g., 90% equity) exposes the
 *   household to outsized losses if that class drops. Diversification is
 *   the "only free lunch in finance" — Harry Markowitz.
 */
@Component
public class AllocationRule implements Rule {

    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("85");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("70");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override public int priority() { return 5; }
    @Override public String category() { return "Asset Allocation"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.assetsByClass() != null
                && !context.assetsByClass().isEmpty()
                && context.totalAssets().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        List<RuleResult> results = new ArrayList<>();

        for (var entry : context.assetsByClass().entrySet()) {
            BigDecimal pct = entry.getValue()
                    .divide(context.totalAssets(), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);

            if (pct.compareTo(CRITICAL_THRESHOLD) > 0) {
                results.add(new RuleResult(
                        "AllocationRule", priority(), "CRITICAL", category(),
                        String.format("%s at %.0f%% — severely over-concentrated", entry.getKey(), pct),
                        Map.of("assetClass", entry.getKey(), "concentration", pct),
                        String.format("Rebalance: reduce %s allocation below 70%% by redirecting new investments to other asset classes",
                                entry.getKey()),
                        "Better risk-adjusted returns through diversification",
                        "A downturn in " + entry.getKey() + " could cause disproportionate portfolio loss"
                ));
            } else if (pct.compareTo(WARNING_THRESHOLD) > 0) {
                results.add(new RuleResult(
                        "AllocationRule", priority(), "WARNING", category(),
                        String.format("%s at %.0f%% — approaching over-concentration", entry.getKey(), pct),
                        Map.of("assetClass", entry.getKey(), "concentration", pct),
                        String.format("Monitor %s allocation; consider diversifying new contributions", entry.getKey()),
                        "Prevent concentration risk from growing",
                        "Continued build-up increases vulnerability to sector-specific downturns"
                ));
            }
        }
        return results;
    }
}
