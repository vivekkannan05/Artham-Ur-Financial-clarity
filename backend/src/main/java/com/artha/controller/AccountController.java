package com.artha.controller;

import com.artha.dto.AccountRequest;
import com.artha.dto.AccountResponse;
import com.artha.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/api/persons/{personId}/accounts")
    public ResponseEntity<AccountResponse> create(@PathVariable UUID personId,
                                                   @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(personId, request));
    }

    @GetMapping("/api/persons/{personId}/accounts")
    public ResponseEntity<List<AccountResponse>> findByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(accountService.findByPersonId(personId));
    }

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @PutMapping("/api/accounts/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    @DeleteMapping("/api/accounts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
