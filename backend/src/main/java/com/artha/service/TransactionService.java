package com.artha.service;

import com.artha.domain.Account;
import com.artha.domain.Holding;
import com.artha.domain.Person;
import com.artha.domain.Transaction;
import com.artha.domain.TransactionType;
import com.artha.dto.TransactionRequest;
import com.artha.dto.TransactionResponse;
import com.artha.repository.AccountRepository;
import com.artha.repository.HoldingRepository;
import com.artha.repository.PersonRepository;
import com.artha.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Transaction operations.
 *
 * IMMUTABILITY: Transactions can only be CREATED and READ — never updated or deleted.
 * This is enforced at the application level (no update/delete methods).
 * In production, the DB user has INSERT+SELECT only on the transaction table.
 *
 * SPEC REF: FR-3.4 (immutability), Review R-1 (DB-level enforcement)
 */
@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              HoldingRepository holdingRepository,
                              PersonRepository personRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Create a new transaction (INSERT only — immutable after creation).
     */
    public TransactionResponse create(UUID personId, TransactionRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
        Holding holding = holdingRepository.findById(request.holdingId())
                .orElseThrow(() -> new RuntimeException("Holding not found: " + request.holdingId()));

        Transaction txn = new Transaction();
        txn.setHolding(holding);
        txn.setOwner(person);
        txn.setAccount(holding.getAccount());
        txn.setTransactionDate(request.transactionDate());
        txn.setAmount(request.amount());
        txn.setTransactionType(request.transactionType());
        txn.setUnits(request.units());
        txn.setNavPrice(request.navPrice());
        txn.setSourceOfFunds(request.sourceOfFunds());
        txn.setNotes(request.notes());
        txn.setGoalId(request.goalId());

        return toResponse(transactionRepository.save(txn));
    }

    /**
     * Read a transaction by ID.
     */
    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        return toResponse(transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id)));
    }

    /**
     * List all transactions for a holding, ordered by date.
     * Used by XIRR/CAGR calculations.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> findByHoldingId(UUID holdingId) {
        return transactionRepository.findByHoldingIdOrderByTransactionDateAsc(holdingId)
                .stream().map(this::toResponse).toList();
    }

    // NOTE: No update() or delete() methods — transactions are IMMUTABLE per FR-3.4

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getHolding().getId(), t.getOwner().getId(),
                t.getAccount().getId(), t.getTransactionDate(), t.getAmount(),
                t.getTransactionType().name(), t.getUnits(), t.getNavPrice(),
                t.getSourceOfFunds(), t.getNotes(), t.getGoalId(), t.getCreatedAt());
    }
}
