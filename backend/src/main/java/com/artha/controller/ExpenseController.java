package com.artha.controller;

import com.artha.dto.ExpenseRequest;
import com.artha.dto.ExpenseResponse;
import com.artha.service.ExpenseService;
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
 * REST Controller for Expense operations.
 *
 * Expense entries are nested under a person:
 *   POST /api/persons/{personId}/expenses   → Add an expense
 *   GET  /api/persons/{personId}/expenses   → List expenses for a person
 *
 * Direct operations by expense ID:
 *   GET    /api/expenses/{id}   → Get one expense
 *   PUT    /api/expenses/{id}   → Update an expense
 *   DELETE /api/expenses/{id}   → Delete an expense
 */
@RestController
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/api/persons/{personId}/expenses")
    public ResponseEntity<ExpenseResponse> create(@PathVariable UUID personId,
                                                   @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(personId, request));
    }

    @GetMapping("/api/persons/{personId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> findByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(expenseService.findByPersonId(personId));
    }

    @GetMapping("/api/expenses/{id}")
    public ResponseEntity<ExpenseResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.findById(id));
    }

    @PutMapping("/api/expenses/{id}")
    public ResponseEntity<ExpenseResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/api/expenses/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
