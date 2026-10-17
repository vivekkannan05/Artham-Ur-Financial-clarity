package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import com.artha.dto.GoalProjection;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contribution Adequacy Rule — Priority 6.
 *
 * SPEC REF: FR-7.1 priority order position 6 — "Contribution Adequacy."
 *
 * WHAT IT CHECKS:
 *   For each goal, if the required monthly SIP to reach the target is
 *   significantly more than what's currently being contributed (indicated
 *   by a shortfall), the goal's funding trajectory is inadequate.
 *
 *   This differs from GoalAdequacyRule (P1) which checks the CURRENT
 *   funding percentage. ContributionRule checks the TRAJECTORY — whether
 *   ongoing contributions are sufficient to close the gap.
 *
 * TRIGGER: Required SIP > 0 AND funding < 100% (there's still a gap).
 */
@Component
public class ContributionRule implements Rule {

    private static final BigDecimal FULL_FUNDING = new BigDecimal("100");

    @Override public int priority() { return 6; }
    @Override public String category() { return "Contribution Adequacy"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.goalProjections() != null && !context.goalProjections().isEmpty();
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        List<RuleResult> results = new ArrayList<>();
        List<GoalProjection> projections = context.goalProjections();
        List<String> names = context.goalNames();

        for (int i = 0; i < projections.size(); i++) {
            GoalProjection p = projections.get(i);
            String goalName = i < names.size() ? names.get(i) : "Goal " + (i + 1);

            // Only fire if goal isn't fully funded AND requires additional SIP
            if (p.fundingPercentage().compareTo(FULL_FUNDING) < 0
                    && p.requiredMonthlySip().compareTo(BigDecimal.ZERO) > 0) {
                results.add(new RuleResult(
                        "ContributionRule", priority(), "INFO", category(),
                        String.format("%s needs ₹%.0f/month SIP to reach target",
                                goalName, p.requiredMonthlySip()),
                        Map.of("goalName", goalName,
                                "requiredSip", p.requiredMonthlySip(),
                                "fundingPct", p.fundingPercentage()),
                        String.format("Start or increase SIP of ₹%.0f/month for %s",
                                p.requiredMonthlySip(), goalName),
                        "Goal reaches target by the deadline",
                        "Shortfall compounds with each missed month of contributions"
                ));
            }
        }
        return results;
    }
}
