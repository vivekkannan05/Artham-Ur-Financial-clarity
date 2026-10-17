package com.artha.advisor.ai;

import com.artha.advisor.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Explainer Service — Layer 3 of the advisor pipeline.
 *
 * SPEC REF: FR-7.1 — "Layer 3 — AI explanation (Spring AI + Ollama) takes the
 * flags and numeric context, produces plain-English explanations."
 * SPEC REF: NFR-5.2 — "AI SHALL only explain and prioritize — it SHALL NOT invent
 * calculations, fabricate data, or override deterministic results."
 *
 * THE 3-LAYER PIPELINE:
 *   Layer 1: Deterministic calculations (XIRR, projections, ratios)  → numeric facts
 *   Layer 2: Rule engine (Java rules)                                → flags + recommendations
 *   Layer 3: AI explanation (THIS SERVICE)                           → plain-English summary
 *
 * PHASE 1 IMPLEMENTATION:
 *   Uses rule-text fallback: constructs human-readable explanations from the
 *   RuleResult fields (trigger, action, impact, risk). This is deterministic
 *   and consistent — it never contradicts the calculation layer.
 *
 * PHASE 2 UPGRADE PATH:
 *   Replace the fallback with Spring AI ChatClient + Ollama:
 *     @Autowired ChatClient chatClient;
 *     String prompt = buildPrompt(ruleResults, numericContext);
 *     String aiResponse = chatClient.prompt(prompt).call().content();
 *     validate(aiResponse, ruleResults);  // NFR-5.2: AI must not contradict calc layer
 *
 * WHY A SEPARATE SERVICE?
 *   Isolating the AI layer means:
 *   1. Rules work without Ollama running (testability)
 *   2. AI can be swapped (Ollama → OpenAI → Claude) without touching rules
 *   3. If AI output is inconsistent, we fall back to rule text (NFR-5.2)
 */
@Service
public class AiExplainerService {

    private static final Logger log = LoggerFactory.getLogger(AiExplainerService.class);

    /**
     * Generate a plain-English explanation for a list of rule results.
     *
     * Phase 1: Deterministic template-based explanation from rule fields.
     * Phase 2: Will call Ollama via Spring AI ChatClient.
     *
     * @param ruleResults The results from the rule engine (Layer 2).
     * @return A human-readable summary suitable for the UI.
     */
    public String explain(List<RuleResult> ruleResults) {
        if (ruleResults == null || ruleResults.isEmpty()) {
            return "Your finances are on track. No issues detected. Keep up the good work!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("We found %d item(s) that need your attention:\n\n",
                ruleResults.size()));

        for (int i = 0; i < ruleResults.size(); i++) {
            RuleResult r = ruleResults.get(i);
            sb.append(String.format("%d. [%s] %s\n", i + 1, r.severity(), r.trigger()));
            sb.append(String.format("   → Action: %s\n", r.action()));
            sb.append(String.format("   → If you act: %s\n", r.expectedImpact()));
            sb.append(String.format("   → If you don't: %s\n\n", r.downsideRisk()));
        }

        // Summary by severity
        long critical = ruleResults.stream().filter(r -> "CRITICAL".equals(r.severity())).count();
        long warnings = ruleResults.stream().filter(r -> "WARNING".equals(r.severity())).count();
        long info = ruleResults.stream().filter(r -> "INFO".equals(r.severity())).count();

        sb.append("Summary: ");
        if (critical > 0) sb.append(String.format("%d critical, ", critical));
        if (warnings > 0) sb.append(String.format("%d warning(s), ", warnings));
        if (info > 0) sb.append(String.format("%d informational", info));

        return sb.toString().trim();
    }

    /**
     * Generate a per-recommendation explanation.
     *
     * Phase 1: Template from rule fields.
     * Phase 2: Ollama call with evidence context.
     */
    public String explainSingle(RuleResult result) {
        return String.format(
                "%s — %s. Recommended action: %s. "
                + "Expected outcome: %s. Risk of inaction: %s.",
                result.category(), result.trigger(), result.action(),
                result.expectedImpact(), result.downsideRisk()
        );
    }
}
