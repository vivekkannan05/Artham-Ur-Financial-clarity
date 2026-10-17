package com.artha.controller;

import com.artha.dto.GoalProjection;
import com.artha.dto.GoalRequest;
import com.artha.dto.GoalResponse;
import com.artha.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("/api/households/{householdId}/goals")
    public ResponseEntity<GoalResponse> create(@PathVariable UUID householdId,
                                                @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(householdId, request));
    }

    @GetMapping("/api/households/{householdId}/goals")
    public ResponseEntity<List<GoalResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(goalService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/goals/{id}")
    public ResponseEntity<GoalResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(goalService.findById(id));
    }

    @PutMapping("/api/goals/{id}")
    public ResponseEntity<GoalResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(id, request));
    }

    @DeleteMapping("/api/goals/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Goal funding projection (per FR-4.2). */
    @GetMapping("/api/goals/{id}/projection")
    public ResponseEntity<GoalProjection> projection(@PathVariable UUID id) {
        return ResponseEntity.ok(goalService.computeProjection(id));
    }
}
