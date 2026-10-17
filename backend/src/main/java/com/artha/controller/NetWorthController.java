package com.artha.controller;

import com.artha.dto.EmergencyFundStatus;
import com.artha.dto.NetWorthResponse;
import com.artha.service.NetWorthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Net Worth and Emergency Fund.
 *
 * SPEC REF: FR-5.2 — Emergency fund adequacy endpoint.
 * SPEC REF: FR-5.3 — Net-worth computation and history endpoints.
 *
 * ENDPOINTS:
 *   GET  /api/households/{id}/networth             — Current net worth (live computation)
 *   POST /api/households/{id}/networth/snapshot     — Take a snapshot (persist)
 *   GET  /api/households/{id}/networth/history      — Snapshot history (for trend chart)
 *   GET  /api/households/{id}/emergency-fund        — Emergency fund status
 */
@RestController
public class NetWorthController {

    private final NetWorthService netWorthService;

    public NetWorthController(NetWorthService netWorthService) {
        this.netWorthService = netWorthService;
    }

    /** Live net-worth computation (not persisted). */
    @GetMapping("/api/households/{householdId}/networth")
    public ResponseEntity<NetWorthResponse> currentNetWorth(@PathVariable UUID householdId) {
        return ResponseEntity.ok(netWorthService.computeCurrentNetWorth(householdId));
    }

    /** Take and persist a net-worth snapshot for today. */
    @PostMapping("/api/households/{householdId}/networth/snapshot")
    public ResponseEntity<NetWorthResponse> takeSnapshot(@PathVariable UUID householdId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(netWorthService.takeSnapshot(householdId));
    }

    /** Get snapshot history (most recent first). */
    @GetMapping("/api/households/{householdId}/networth/history")
    public ResponseEntity<List<NetWorthResponse>> history(@PathVariable UUID householdId) {
        return ResponseEntity.ok(netWorthService.getHistory(householdId));
    }

    /** Emergency fund adequacy status (per FR-5.2). */
    @GetMapping("/api/households/{householdId}/emergency-fund")
    public ResponseEntity<EmergencyFundStatus> emergencyFund(@PathVariable UUID householdId) {
        return ResponseEntity.ok(netWorthService.computeEmergencyFund(householdId));
    }
}
