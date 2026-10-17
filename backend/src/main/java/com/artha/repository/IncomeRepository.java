package com.artha.repository;

import com.artha.domain.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Income entities.
 */
@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

    /**
     * Find all income entries for a specific person.
     */
    List<Income> findByPersonId(UUID personId);

    /**
     * Find all income entries for all members of a household.
     * Used by CashFlowService to compute household-level cash flow.
     *
     * Spring Data generates: SELECT i FROM Income i WHERE i.person.household.id = ?
     * (navigates the Person → Household relationship automatically)
     */
    List<Income> findByPersonHouseholdId(UUID householdId);
}
