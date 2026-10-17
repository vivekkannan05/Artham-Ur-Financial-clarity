package com.artha.controller;

import com.artha.dto.LiabilityRequest;
import com.artha.dto.LiabilityResponse;
import com.artha.service.LiabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Liability management.
 *
 * SPEC REF: FR-5.1 — Full CRUD for household liabilities.
 *
 * ENDPOINTS:
 *   POST   /api/households/{householdId}/liabilities  — Create a liability
 *   GET    /api/households/{householdId}/liabilities  — List all for household
 *   GET    /api/liabilities/{id}                       — Get by ID
 *   PUT    /api/liabilities/{id}                       — Update
 *   DELETE /api/liabilities/{id}                       — Delete
 */
@RestController
public class LiabilityController {

    private final LiabilityService liabilityService;

    public LiabilityController(LiabilityService liabilityService) {
        this.liabilityService = liabilityService;
    }

    @PostMapping("/api/households/{householdId}/liabilities")
    public ResponseEntity<LiabilityResponse> create(@PathVariable UUID householdId,
                                                     @Valid @RequestBody LiabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(liabilityService.create(householdId, request));
    }

    @GetMapping("/api/households/{householdId}/liabilities")
    public ResponseEntity<List<LiabilityResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(liabilityService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/liabilities/{id}")
    public ResponseEntity<LiabilityResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(liabilityService.findById(id));
    }

    @PutMapping("/api/liabilities/{id}")
    public ResponseEntity<LiabilityResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody LiabilityRequest request) {
        return ResponseEntity.ok(liabilityService.update(id, request));
    }

    @DeleteMapping("/api/liabilities/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        liabilityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
