package com.artha.repository;

import com.artha.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * Repository for Household entities.
 *
 * HOW SPRING DATA JPA WORKS:
 *   By extending JpaRepository, Spring automatically provides these methods:
 *   - save(entity)        → INSERT or UPDATE
 *   - findById(id)        → SELECT WHERE id = ?
 *   - findAll()           → SELECT * (with pagination support)
 *   - deleteById(id)      → DELETE WHERE id = ?
 *   - count()             → SELECT COUNT(*)
 *
 *   You can also add custom query methods by naming convention:
 *   - findByName(name)    → SELECT WHERE name = ?
 *   - findByStatus(status) → SELECT WHERE status = ?
 *   Spring generates the SQL from the method name. No SQL to write.
 *
 * @Repository is optional here (JpaRepository is auto-detected),
 * but we include it for clarity.
 */
@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    // JpaRepository provides all basic CRUD operations.
    // Custom query methods will be added as needed.
}
