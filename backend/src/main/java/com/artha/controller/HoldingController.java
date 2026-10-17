package com.artha.controller;

import com.artha.dto.HoldingRequest;
import com.artha.dto.HoldingResponse;
import com.artha.dto.InvestmentAnalytics;
import com.artha.service.HoldingService;
import com.artha.service.InvestmentAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class HoldingController {

    private final HoldingService holdingService;
    private final InvestmentAnalyticsService analyticsService;

    public HoldingController(HoldingService holdingService, InvestmentAnalyticsService analyticsService) {
        this.holdingService = holdingService;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/api/persons/{personId}/holdings")
    public ResponseEntity<HoldingResponse> create(@PathVariable UUID personId,
                                                   @Valid @RequestBody HoldingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holdingService.create(personId, request));
    }

    @GetMapping("/api/persons/{personId}/holdings")
    public ResponseEntity<List<HoldingResponse>> findByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(holdingService.findByPersonId(personId));
    }

    @GetMapping("/api/accounts/{accountId}/holdings")
    public ResponseEntity<List<HoldingResponse>> findByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(holdingService.findByAccountId(accountId));
    }

    /** All holdings across all members of a household — used by the portfolio page. */
    @GetMapping("/api/households/{householdId}/holdings")
    public ResponseEntity<List<HoldingResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(holdingService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/holdings/{id}")
    public ResponseEntity<HoldingResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(holdingService.findById(id));
    }

    @PutMapping("/api/holdings/{id}")
    public ResponseEntity<HoldingResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody HoldingRequest request) {
        return ResponseEntity.ok(holdingService.update(id, request));
    }

    @DeleteMapping("/api/holdings/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        holdingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Compute XIRR, CAGR, absolute return for a holding (per FR-3.6) */
    @GetMapping("/api/holdings/{id}/analytics")
    public ResponseEntity<InvestmentAnalytics> analytics(@PathVariable UUID id) {
        return ResponseEntity.ok(analyticsService.computeForHolding(id));
    }
}
