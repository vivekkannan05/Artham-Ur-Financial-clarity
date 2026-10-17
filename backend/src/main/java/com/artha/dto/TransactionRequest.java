package com.artha.dto;

import com.artha.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull(message = "Holding ID is required")
        UUID holdingId,
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,
        BigDecimal units,
        BigDecimal navPrice,
        String sourceOfFunds,
        String notes,
        UUID goalId
) {}
