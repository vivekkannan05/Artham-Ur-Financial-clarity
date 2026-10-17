package com.artha.controller;

import com.artha.dto.PropertyRequest;
import com.artha.dto.PropertyResponse;
import com.artha.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/** REST controller for Property management. SPEC REF: FR-6.3 */
@RestController
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping("/api/households/{householdId}/properties")
    public ResponseEntity<PropertyResponse> create(@PathVariable UUID householdId,
                                                    @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.create(householdId, request));
    }

    @GetMapping("/api/households/{householdId}/properties")
    public ResponseEntity<List<PropertyResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(propertyService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/properties/{id}")
    public ResponseEntity<PropertyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.findById(id));
    }

    @PutMapping("/api/properties/{id}")
    public ResponseEntity<PropertyResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(propertyService.update(id, request));
    }

    @DeleteMapping("/api/properties/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
