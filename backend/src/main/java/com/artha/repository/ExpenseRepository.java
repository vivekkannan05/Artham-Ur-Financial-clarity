package com.artha.repository;

import com.artha.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Expense entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Find all expense entries for a specific person.
     */
    List<Expense> findByPersonId(UUID personId);

    /**
     * Find all expense entries for all members of a household.
     * Used by CashFlowService to compute household-level cash flow.
     */
    List<Expense> findByPersonHouseholdId(UUID householdId);
}
