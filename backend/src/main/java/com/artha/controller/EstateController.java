package com.artha.controller;

import com.artha.domain.*;
import com.artha.repository.AccountRepository;
import com.artha.repository.EstateInfoRepository;
import com.artha.repository.HoldingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Estate/Succession metadata.
 *
 * SPEC REF: FR-7.5 — "Track estate and succession metadata per asset/account:
 * nominee name, joint holder(s), beneficiary designation, will status."
 */
@RestController
@RequestMapping("/api/estate")
public class EstateController {

    private final EstateInfoRepository estateRepository;
    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;

    public EstateController(EstateInfoRepository estateRepository,
                            HoldingRepository holdingRepository,
                            AccountRepository accountRepository) {
        this.estateRepository = estateRepository;
        this.holdingRepository = holdingRepository;
        this.accountRepository = accountRepository;
    }

    /** Get estate info for a specific holding. */
    @GetMapping("/holding/{holdingId}")
    public ResponseEntity<List<EstateInfo>> getByHolding(@PathVariable UUID holdingId) {
        return ResponseEntity.ok(estateRepository.findByHoldingId(holdingId));
    }

    /** Get estate info for a specific account. */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<EstateInfo>> getByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(estateRepository.findByAccountId(accountId));
    }

    /** Create estate info for a holding. */
    @PostMapping("/holding/{holdingId}")
    public ResponseEntity<EstateInfo> createForHolding(
            @PathVariable UUID holdingId, @RequestBody EstateInfo body) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new RuntimeException("Holding not found: " + holdingId));
        body.setHolding(holding);
        return ResponseEntity.status(HttpStatus.CREATED).body(estateRepository.save(body));
    }

    /** Create estate info for an account. */
    @PostMapping("/account/{accountId}")
    public ResponseEntity<EstateInfo> createForAccount(
            @PathVariable UUID accountId, @RequestBody EstateInfo body) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        body.setAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(estateRepository.save(body));
    }

    /** Update estate info. */
    @PutMapping("/{id}")
    public ResponseEntity<EstateInfo> update(@PathVariable UUID id, @RequestBody EstateInfo body) {
        EstateInfo existing = estateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estate info not found: " + id));
        existing.setNomineeName(body.getNomineeName());
        existing.setJointHoldersJson(body.getJointHoldersJson());
        existing.setBeneficiaryDesignation(body.getBeneficiaryDesignation());
        existing.setWillStatus(body.getWillStatus());
        return ResponseEntity.ok(estateRepository.save(existing));
    }

    /** Delete estate info. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        estateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
