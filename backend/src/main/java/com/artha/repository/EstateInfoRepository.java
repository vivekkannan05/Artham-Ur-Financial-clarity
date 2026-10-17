package com.artha.repository;

import com.artha.domain.EstateInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EstateInfoRepository extends JpaRepository<EstateInfo, UUID> {
    List<EstateInfo> findByHoldingId(UUID holdingId);
    List<EstateInfo> findByAccountId(UUID accountId);
}
