package com.artha.controller;

import com.artha.dto.InsuranceCoverageGap.CoverageAnalysis;
import com.artha.dto.InsurancePolicyRequest;
import com.artha.dto.InsurancePolicyResponse;
import com.artha.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Insurance policies and coverage gap analysis.
 *
 * SPEC REF: FR-6.2
 */
@RestController
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @PostMapping("/api/households/{householdId}/insurance")
    public ResponseEntity<InsurancePolicyResponse> create(@PathVariable UUID householdId,
                                                           @Valid @RequestBody InsurancePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(insuranceService.create(householdId, request));
    }

    @GetMapping("/api/households/{householdId}/insurance")
    public ResponseEntity<List<InsurancePolicyResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(insuranceService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/insurance/{id}")
    public ResponseEntity<InsurancePolicyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(insuranceService.findById(id));
    }

    @PutMapping("/api/insurance/{id}")
    public ResponseEntity<InsurancePolicyResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody InsurancePolicyRequest request) {
        return ResponseEntity.ok(insuranceService.update(id, request));
    }

    @DeleteMapping("/api/insurance/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        insuranceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Coverage gap analysis (FR-6.2). */
    @GetMapping("/api/households/{householdId}/insurance/coverage-gaps")
    public ResponseEntity<CoverageAnalysis> coverageGaps(@PathVariable UUID householdId) {
        return ResponseEntity.ok(insuranceService.analyzeCoverageGaps(householdId));
    }
}
