package com.artha.repository;

import com.artha.domain.Liability;
import com.artha.domain.LiabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Liability entities.
 *
 * SPEC REF: FR-5.1 — Liability CRUD + queries for cash-flow and net-worth calculations.
 *
 * WHY THESE QUERIES?
 *   - findByHouseholdId: Show all liabilities on the liabilities screen.
 *   - findByHouseholdIdAndStatus: Net-worth calculation only includes ACTIVE liabilities.
 *     Closed/defaulted liabilities are historical records, not current debts.
 */
@Repository
public interface LiabilityRepository extends JpaRepository<Liability, UUID> {

    List<Liability> findByHouseholdId(UUID householdId);

    List<Liability> findByHouseholdIdAndStatus(UUID householdId, LiabilityStatus status);
}
