package com.artha.repository;

import com.artha.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByHouseholdId(UUID householdId);
    List<Goal> findByHouseholdIdAndStatus(UUID householdId, com.artha.domain.GoalStatus status);
}
