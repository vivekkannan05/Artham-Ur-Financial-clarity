package com.artha.controller;

import com.artha.dto.ScenarioRequest;
import com.artha.dto.ScenarioResult;
import com.artha.dto.TaxFlag;
import com.artha.service.ScenarioService;
import com.artha.service.TaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Tax Center and Scenario Analysis.
 *
 * SPEC REF: FR-6.1 (tax flags), FR-6.4 (scenario analysis)
 */
@RestController
public class TaxAndScenarioController {

    private final TaxService taxService;
    private final ScenarioService scenarioService;

    public TaxAndScenarioController(TaxService taxService, ScenarioService scenarioService) {
        this.taxService = taxService;
        this.scenarioService = scenarioService;
    }

    /** Tax flag scan (FR-6.1). */
    @GetMapping("/api/households/{householdId}/tax/flags")
    public ResponseEntity<List<TaxFlag>> taxFlags(@PathVariable UUID householdId) {
        return ResponseEntity.ok(taxService.scanForFlags(householdId));
    }

    /** Scenario analysis — what-if modeling (FR-6.4). */
    @PostMapping("/api/households/{householdId}/scenario")
    public ResponseEntity<ScenarioResult> scenario(@PathVariable UUID householdId,
                                                    @RequestBody ScenarioRequest request) {
        return ResponseEntity.ok(scenarioService.analyze(householdId, request));
    }
}
