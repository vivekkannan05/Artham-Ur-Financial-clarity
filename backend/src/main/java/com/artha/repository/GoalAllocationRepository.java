package com.artha.repository;

import com.artha.domain.GoalAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalAllocationRepository extends JpaRepository<GoalAllocation, UUID> {
    List<GoalAllocation> findByGoalId(UUID goalId);
    List<GoalAllocation> findByHoldingId(UUID holdingId);

    /** Sum allocation percentages for a holding across all goals (must equal 100). */
    @Query("SELECT COALESCE(SUM(ga.allocationPercentage), 0) FROM GoalAllocation ga WHERE ga.holding.id = :holdingId")
    BigDecimal sumAllocationByHoldingId(UUID holdingId);
}
