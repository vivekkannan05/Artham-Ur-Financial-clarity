package com.artha.controller;

import com.artha.advisor.RuleResult;
import com.artha.dto.HealthScore;
import com.artha.service.AdvisorService;
import com.artha.service.HealthScoreService;
import com.artha.service.StaleDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Advisor engine, Health Score, and Data Quality.
 * SPEC REF: FR-7.1, FR-7.3, FR-7.7
 */
@RestController
public class AdvisorController {

    private final AdvisorService advisorService;
    private final HealthScoreService healthScoreService;
    private final StaleDataService staleDataService;

    public AdvisorController(AdvisorService advisorService,
                             HealthScoreService healthScoreService,
                             StaleDataService staleDataService) {
        this.advisorService = advisorService;
        this.healthScoreService = healthScoreService;
        this.staleDataService = staleDataService;
    }

    /** Run the advisor pipeline (calc → rules → recommendations). */
    @GetMapping("/api/households/{householdId}/advisor")
    public ResponseEntity<List<RuleResult>> advise(@PathVariable UUID householdId) {
        return ResponseEntity.ok(advisorService.advise(householdId));
    }

    /** Run advisor and persist recommendations. */
    @PostMapping("/api/households/{householdId}/advisor")
    public ResponseEntity<List<RuleResult>> adviseAndStore(@PathVariable UUID householdId) {
        return ResponseEntity.ok(advisorService.adviseAndStore(householdId));
    }

    /** Financial health score (7 sub-scores). */
    @GetMapping("/api/households/{householdId}/health-score")
    public ResponseEntity<HealthScore> healthScore(@PathVariable UUID householdId) {
        var context = advisorService.buildContext(householdId);
        return ResponseEntity.ok(healthScoreService.compute(context));
    }

    /** Stale data detection — holdings not updated in > 90 days (FR-7.7). */
    @GetMapping("/api/households/{householdId}/stale-data")
    public ResponseEntity<List<StaleDataService.StaleRecord>> staleData(
            @PathVariable UUID householdId) {
        return ResponseEntity.ok(staleDataService.findStaleHoldings(householdId));
    }
}
