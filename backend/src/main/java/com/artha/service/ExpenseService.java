package com.artha.service;

import com.artha.domain.Expense;
import com.artha.domain.Person;
import com.artha.dto.ExpenseRequest;
import com.artha.dto.ExpenseResponse;
import com.artha.repository.ExpenseRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Expense CRUD operations.
 *
 * SPEC REF: FR-2.2 — Expense recording with consumption/investment classification.
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;

    public ExpenseService(ExpenseRepository expenseRepository, PersonRepository personRepository) {
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
    }

    public ExpenseResponse create(UUID personId, ExpenseRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));

        Expense expense = new Expense();
        expense.setPerson(person);
        expense.setName(request.name());
        expense.setAmount(request.amount());
        expense.setFrequency(request.frequency());
        expense.setCategory(request.category());
        expense.setStartDate(request.startDate());
        expense.setEndDate(request.endDate());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByPersonId(UUID personId) {
        return expenseRepository.findByPersonId(personId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));

        expense.setName(request.name());
        expense.setAmount(request.amount());
        expense.setFrequency(request.frequency());
        expense.setCategory(request.category());
        expense.setStartDate(request.startDate());
        expense.setEndDate(request.endDate());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Expense not found: " + id);
        }
        expenseRepository.deleteById(id);
    }

    // ─── Mapping ─────────────────────────────────────────────

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getPerson().getId(),
                expense.getName(),
                expense.getAmount(),
                expense.getFrequency().name(),
                expense.getCategory().name(),
                expense.getStartDate(),
                expense.getEndDate(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
