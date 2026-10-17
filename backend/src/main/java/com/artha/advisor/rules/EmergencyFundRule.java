package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import com.artha.domain.EmergencyFundAdequacy;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Emergency Fund Rule — Priority 2.
 * SPEC: "Months covered < target (default 6)." (Design Doc Sec 9.5)
 */
@Component
public class EmergencyFundRule implements Rule {

    @Override public int priority() { return 2; }
    @Override public String category() { return "Emergency Fund"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.emergencyFund() != null;
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        var ef = context.emergencyFund();
        if (ef.adequacy() == EmergencyFundAdequacy.ADEQUATE) {
            return List.of();
        }

        String severity = ef.adequacy() == EmergencyFundAdequacy.CRITICAL ? "CRITICAL" : "WARNING";

        return List.of(new RuleResult(
                "EmergencyFundRule", priority(), severity, category(),
                String.format("Emergency fund covers %.1f months — target is 6 months",
                        ef.monthsCovered()),
                Map.of("monthsCovered", ef.monthsCovered(), "shortfall", ef.shortfall(),
                        "adequacy", ef.adequacy().name()),
                String.format("Build liquid reserves of ₹%.0f to cover the shortfall", ef.shortfall()),
                "6 months of expenses covered, protecting against job loss or medical emergency",
                "Without adequate reserves, any financial shock requires breaking long-term investments"
        ));
    }
}
