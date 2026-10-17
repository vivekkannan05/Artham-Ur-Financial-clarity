package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import com.artha.dto.InsuranceCoverageGap;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Insurance Gap Rule — Priority 4.
 * SPEC: "Term life < 10× income OR health < ₹10L." (Design Doc Sec 9.5)
 */
@Component
public class InsuranceGapRule implements Rule {

    @Override public int priority() { return 4; }
    @Override public String category() { return "Insurance Gaps"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.insuranceGaps() != null && !context.insuranceGaps().isEmpty();
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        return context.insuranceGaps().stream()
                .map(gap -> new RuleResult(
                        "InsuranceGapRule", priority(), "WARNING", category(),
                        gap.message(),
                        Map.of("gapType", gap.gapType(), "recommended", gap.recommended(),
                                "actual", gap.actual(), "shortfall", gap.shortfall()),
                        String.format("Increase %s coverage by ₹%.0f", gap.gapType(), gap.shortfall()),
                        "Adequate protection against financial risk",
                        "Underinsurance exposes the family to catastrophic financial loss"
                ))
                .toList();
    }
}
