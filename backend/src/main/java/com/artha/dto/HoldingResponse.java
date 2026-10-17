package com.artha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HoldingResponse(
        UUID id,
        String name,
        UUID accountId,
        UUID ownerId,
        String instrumentTypeCode,
        String instrumentTypeName,
        String assetClass,
        BigDecimal units,
        BigDecimal acquisitionCostTotal,
        BigDecimal acquisitionCostPerUnit,
        LocalDate acquisitionDate,
        BigDecimal currentValue,
        LocalDate valuationDate,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
