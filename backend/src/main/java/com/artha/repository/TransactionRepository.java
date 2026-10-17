package com.artha.repository;

import com.artha.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    /** All transactions for a holding, ordered by date (for XIRR calculation). */
    List<Transaction> findByHoldingIdOrderByTransactionDateAsc(UUID holdingId);
    List<Transaction> findByOwnerId(UUID ownerId);
    /** All transactions for a household (navigates holding → account → owner → household). */
    List<Transaction> findByHoldingAccountOwnerHouseholdId(UUID householdId);
}
