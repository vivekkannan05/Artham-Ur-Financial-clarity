package com.artha.repository;

import com.artha.domain.Holding;
import com.artha.domain.HoldingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByAccountId(UUID accountId);
    List<Holding> findByOwnerId(UUID ownerId);
    List<Holding> findByOwnerHouseholdId(UUID householdId);
    List<Holding> findByOwnerHouseholdIdAndStatus(UUID householdId, HoldingStatus status);
}
