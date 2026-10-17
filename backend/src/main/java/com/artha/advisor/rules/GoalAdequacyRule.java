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
 * Goal Adequacy Rule — Priority 1.
 * SPEC: "Any goal funding < 80% of projected need." (Design Doc Sec 9.5)
 */
@Component
public class GoalAdequacyRule implements Rule {

    private static final BigDecimal FUNDING_THRESHOLD = new BigDecimal("80.00");

    @Override public int priority() { return 1; }
    @Override public String category() { return "Goal Adequacy"; }

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

            if (p.fundingPercentage().compareTo(FUNDING_THRESHOLD) < 0) {
                results.add(new RuleResult(
                        "GoalAdequacyRule", priority(), "CRITICAL", category(),
                        String.format("%s is only %.1f%% funded", goalName, p.fundingPercentage()),
                        Map.of("goalName", goalName, "fundingPct", p.fundingPercentage(),
                                "shortfall", p.shortfallOrSurplus()),
                        String.format("Increase SIP for %s by ₹%.0f/month or extend target date",
                                goalName, p.requiredMonthlySip()),
                        "Reaching the goal target on time",
                        "Shortfall may grow with inflation"
                ));
            }
        }
        return results;
    }
}
