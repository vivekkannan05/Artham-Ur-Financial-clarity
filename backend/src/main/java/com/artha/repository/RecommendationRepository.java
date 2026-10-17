package com.artha.repository;

import com.artha.domain.Recommendation;
import com.artha.domain.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    List<Recommendation> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    List<Recommendation> findByHouseholdIdAndStatus(UUID householdId, RecommendationStatus status);
}
