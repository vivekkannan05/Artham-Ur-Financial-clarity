package com.artha.repository;

import com.artha.domain.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentTypeRepository extends JpaRepository<InstrumentType, UUID> {
    Optional<InstrumentType> findByCode(String code);
}
