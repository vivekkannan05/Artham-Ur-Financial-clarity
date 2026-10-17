package com.artha.controller;

import com.artha.dto.HouseholdRequest;
import com.artha.dto.HouseholdResponse;
import com.artha.service.HouseholdService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Household operations.
 *
 * URL PREFIX: /api/households
 *
 * ENDPOINTS:
 *   POST   /api/households         → Create a new household
 *   GET    /api/households         → List all households
 *   GET    /api/households/{id}    → Get one household (with members)
 *   PUT    /api/households/{id}    → Update a household
 *   DELETE /api/households/{id}    → Delete a household
 *
 * HOW @RestController WORKS:
 *   Combines @Controller + @ResponseBody. Every method's return value is
 *   automatically serialized to JSON (via Jackson) and sent in the HTTP response.
 *   No need for explicit JSON conversion.
 *
 * HOW @Valid WORKS:
 *   When placed before @RequestBody, Spring validates the DTO using Jakarta
 *   Bean Validation annotations (@NotBlank, @NotNull, @Positive, etc.).
 *   If validation fails, Spring returns HTTP 400 with error details.
 */
@RestController
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> create(@Valid @RequestBody HouseholdRequest request) {
        HouseholdResponse response = householdService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<HouseholdResponse>> findAll() {
        return ResponseEntity.ok(householdService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(householdService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HouseholdResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody HouseholdRequest request) {
        return ResponseEntity.ok(householdService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        householdService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
