package com.artha.controller;

import com.artha.dto.TransactionRequest;
import com.artha.dto.TransactionResponse;
import com.artha.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Transaction operations.
 *
 * IMMUTABILITY: Only POST (create) and GET (read) are exposed.
 * There are NO PUT or DELETE endpoints — transactions cannot be modified.
 * Use the correction endpoint to record adjustments.
 *
 * SPEC REF: FR-3.4
 */
@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Create a new transaction (immutable after creation). */
    @PostMapping("/api/persons/{personId}/transactions")
    public ResponseEntity<TransactionResponse> create(@PathVariable UUID personId,
                                                       @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(personId, request));
    }

    /** Get a transaction by ID. */
    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    /** List all transactions for a holding (ordered by date). */
    @GetMapping("/api/holdings/{holdingId}/transactions")
    public ResponseEntity<List<TransactionResponse>> findByHolding(@PathVariable UUID holdingId) {
        return ResponseEntity.ok(transactionService.findByHoldingId(holdingId));
    }

    // NOTE: No PUT or DELETE — transactions are IMMUTABLE per FR-3.4
}
