package com.artha.controller;

import com.artha.advisor.RuleResult;
import com.artha.dto.HealthScore;
import com.artha.service.AdvisorService;
import com.artha.service.HealthScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Advisor engine and Health Score.
 * SPEC REF: FR-7.1, FR-7.3
 */
@RestController
public class AdvisorController {

    private final AdvisorService advisorService;
    private final HealthScoreService healthScoreService;

    public AdvisorController(AdvisorService advisorService, HealthScoreService healthScoreService) {
        this.advisorService = advisorService;
        this.healthScoreService = healthScoreService;
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
}
