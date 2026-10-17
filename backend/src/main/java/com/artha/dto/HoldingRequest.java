package com.artha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HoldingRequest(
        @NotBlank(message = "Holding name is required")
        String name,
        @NotNull(message = "Account ID is required")
        UUID accountId,
        @NotNull(message = "Instrument type code is required")
        String instrumentTypeCode,
        BigDecimal units,
        BigDecimal acquisitionCostTotal,
        BigDecimal acquisitionCostPerUnit,
        LocalDate acquisitionDate,
        BigDecimal currentValue,
        LocalDate valuationDate,
        String currency
) {}
