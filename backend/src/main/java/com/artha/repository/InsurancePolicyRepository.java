package com.artha.repository;

import com.artha.domain.InsurancePolicy;
import com.artha.domain.InsurancePolicyType;
import com.artha.domain.RenewalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for InsurancePolicy entities.
 *
 * SPEC REF: FR-6.2 — Insurance CRUD and coverage gap queries.
 */
@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, UUID> {

    List<InsurancePolicy> findByHouseholdId(UUID householdId);

    List<InsurancePolicy> findByHouseholdIdAndPolicyType(UUID householdId, InsurancePolicyType type);

    List<InsurancePolicy> findByHouseholdIdAndRenewalStatus(UUID householdId, RenewalStatus status);

    List<InsurancePolicy> findByInsuredPersonId(UUID personId);
}
