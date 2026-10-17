package com.artha.controller;

import com.artha.dto.IncomeRequest;
import com.artha.dto.IncomeResponse;
import com.artha.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Income operations.
 *
 * Income entries are nested under a person:
 *   POST /api/persons/{personId}/incomes   → Add an income source
 *   GET  /api/persons/{personId}/incomes   → List income sources for a person
 *
 * Direct operations by income ID:
 *   GET    /api/incomes/{id}   → Get one income
 *   PUT    /api/incomes/{id}   → Update an income
 *   DELETE /api/incomes/{id}   → Delete an income
 */
@RestController
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping("/api/persons/{personId}/incomes")
    public ResponseEntity<IncomeResponse> create(@PathVariable UUID personId,
                                                  @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incomeService.create(personId, request));
    }

    @GetMapping("/api/persons/{personId}/incomes")
    public ResponseEntity<List<IncomeResponse>> findByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(incomeService.findByPersonId(personId));
    }

    @GetMapping("/api/incomes/{id}")
    public ResponseEntity<IncomeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(incomeService.findById(id));
    }

    @PutMapping("/api/incomes/{id}")
    public ResponseEntity<IncomeResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody IncomeRequest request) {
        return ResponseEntity.ok(incomeService.update(id, request));
    }

    @DeleteMapping("/api/incomes/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incomeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
