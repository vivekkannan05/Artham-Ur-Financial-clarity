package com.artha.repository;

import com.artha.domain.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NetWorthSnapshot entities.
 *
 * SPEC REF: FR-5.3 — "Net-worth history at monthly and yearly granularity."
 *
 * WHY THESE QUERIES?
 *   - findByHouseholdIdOrderBySnapshotDateDesc: Display net-worth history
 *     (most recent first) on the dashboard trend chart.
 *   - findTopByHouseholdIdOrderBySnapshotDateDesc: Get the latest snapshot
 *     for MoM/YoY comparison when creating a new snapshot.
 *   - findByHouseholdIdAndSnapshotDate: Look up a specific month's snapshot
 *     (e.g., same month last year for YoY calculation).
 */
@Repository
public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, UUID> {

    List<NetWorthSnapshot> findByHouseholdIdOrderBySnapshotDateDesc(UUID householdId);

    Optional<NetWorthSnapshot> findTopByHouseholdIdOrderBySnapshotDateDesc(UUID householdId);

    Optional<NetWorthSnapshot> findByHouseholdIdAndSnapshotDate(UUID householdId, LocalDate snapshotDate);
}
