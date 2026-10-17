package com.artha.service;

import com.artha.domain.Income;
import com.artha.domain.Person;
import com.artha.dto.IncomeRequest;
import com.artha.dto.IncomeResponse;
import com.artha.repository.IncomeRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Income CRUD operations.
 *
 * SPEC REF: FR-2.1 — Income recording by person and period.
 */
@Service
@Transactional
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final PersonRepository personRepository;

    public IncomeService(IncomeRepository incomeRepository, PersonRepository personRepository) {
        this.incomeRepository = incomeRepository;
        this.personRepository = personRepository;
    }

    public IncomeResponse create(UUID personId, IncomeRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));

        Income income = new Income();
        income.setPerson(person);
        income.setIncomeType(request.incomeType());
        income.setAmount(request.amount());
        income.setFrequency(request.frequency());
        income.setStartDate(request.startDate());
        income.setEndDate(request.endDate());
        income.setSource(request.source());

        Income saved = incomeRepository.save(income);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public IncomeResponse findById(UUID id) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found: " + id));
        return toResponse(income);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> findByPersonId(UUID personId) {
        return incomeRepository.findByPersonId(personId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IncomeResponse update(UUID id, IncomeRequest request) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Income not found: " + id));

        income.setIncomeType(request.incomeType());
        income.setAmount(request.amount());
        income.setFrequency(request.frequency());
        income.setStartDate(request.startDate());
        income.setEndDate(request.endDate());
        income.setSource(request.source());

        Income saved = incomeRepository.save(income);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (!incomeRepository.existsById(id)) {
            throw new RuntimeException("Income not found: " + id);
        }
        incomeRepository.deleteById(id);
    }

    // ─── Mapping ─────────────────────────────────────────────

    private IncomeResponse toResponse(Income income) {
        return new IncomeResponse(
                income.getId(),
                income.getPerson().getId(),
                income.getIncomeType().name(),
                income.getAmount(),
                income.getFrequency().name(),
                income.getStartDate(),
                income.getEndDate(),
                income.getSource(),
                income.getCreatedAt(),
                income.getUpdatedAt()
        );
    }
}
