package com.artha.repository;

import com.artha.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Property entities.
 *
 * SPEC REF: FR-6.3 — Property CRUD.
 */
@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByHouseholdId(UUID householdId);
}
