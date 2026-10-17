package com.artha.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Financial health score with 7 explainable sub-scores.
 * SPEC REF: FR-7.3 — "Each sub-score has value (0–100), status (Green/Yellow/Red), explanation."
 */
public record HealthScore(
        BigDecimal overallScore,
        String overallStatus,
        List<SubScore> subScores
) {
    public record SubScore(
            String category,
            BigDecimal score,
            String status,       // Green, Yellow, Red
            String explanation
    ) {}
}
