package com.artha.repository;

import com.artha.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Person entities.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {

    /**
     * Find all members of a household.
     * Spring Data generates: SELECT * FROM person WHERE household_id = ?
     */
    List<Person> findByHouseholdId(UUID householdId);
}
