package com.artha.repository;

import com.artha.domain.TransactionCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionCorrectionRepository extends JpaRepository<TransactionCorrection, UUID> {
    List<TransactionCorrection> findByOriginalTransactionId(UUID transactionId);
}
