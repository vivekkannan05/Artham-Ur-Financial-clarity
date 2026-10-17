package com.artha.controller;

import com.artha.dto.CashFlowStatement;
import com.artha.service.CashFlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/**
 * REST Controller for Cash Flow operations.
 *
 * SPEC REF: FR-2.3 — Cash-flow statement per household.
 *
 * ENDPOINT:
 *   GET /api/households/{householdId}/cash-flow → Monthly cash-flow statement
 */
@RestController
@RequestMapping("/api/households/{householdId}/cash-flow")
public class CashFlowController {

    private final CashFlowService cashFlowService;

    public CashFlowController(CashFlowService cashFlowService) {
        this.cashFlowService = cashFlowService;
    }

    /**
     * Compute and return the monthly cash-flow statement for a household.
     *
     * This is a read-only computation — no data is written.
     * The result is computed on-the-fly from current income/expense data.
     */
    @GetMapping
    public ResponseEntity<CashFlowStatement> getMonthlyCashFlow(@PathVariable UUID householdId) {
        return ResponseEntity.ok(cashFlowService.computeMonthlyCashFlow(householdId));
    }
}
