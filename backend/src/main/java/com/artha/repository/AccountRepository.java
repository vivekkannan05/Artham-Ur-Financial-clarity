package com.artha.repository;

import com.artha.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByOwnerId(UUID ownerId);
    List<Account> findByOwnerHouseholdId(UUID householdId);
}
