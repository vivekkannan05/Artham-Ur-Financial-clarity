package com.artha.advisor.rules;

import com.artha.advisor.HouseholdFinancialContext;
import com.artha.advisor.Rule;
import com.artha.advisor.RuleResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Debt Management Rule — Priority 3.
 * SPEC: "Debt-service ratio > 40%." (Design Doc Sec 9.5)
 */
@Component
public class DebtManagementRule implements Rule {

    private static final BigDecimal DSR_THRESHOLD = new BigDecimal("40.00");

    @Override public int priority() { return 3; }
    @Override public String category() { return "Debt Management"; }

    @Override
    public boolean appliesTo(HouseholdFinancialContext context) {
        return context.debtServiceRatio() != null
                && context.debtServiceRatio().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public List<RuleResult> evaluate(HouseholdFinancialContext context) {
        BigDecimal dsr = context.debtServiceRatio();
        if (dsr.compareTo(DSR_THRESHOLD) <= 0) {
            return List.of();
        }

        return List.of(new RuleResult(
                "DebtManagementRule", priority(), "CRITICAL", category(),
                String.format("Debt-service ratio is %.1f%% — above the 40%% threshold", dsr),
                Map.of("debtServiceRatio", dsr, "threshold", DSR_THRESHOLD),
                "Consider prepaying the highest-rate loan or restructuring EMIs",
                "Lower monthly outflow, improved cash flow health",
                "High debt burden leaves no buffer for emergencies or investment"
        ));
    }
}
