package com.artha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID holdingId,
        UUID ownerId,
        UUID accountId,
        LocalDate transactionDate,
        BigDecimal amount,
        String transactionType,
        BigDecimal units,
        BigDecimal navPrice,
        String sourceOfFunds,
        String notes,
        UUID goalId,
        Instant createdAt
) {}
