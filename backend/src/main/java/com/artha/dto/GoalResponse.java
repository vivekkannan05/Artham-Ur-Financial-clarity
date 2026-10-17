package com.artha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id, UUID householdId, String name, String goalType,
        UUID beneficiaryId, UUID ownerId,
        LocalDate targetDate, BigDecimal targetAmountToday,
        BigDecimal inflationRate, BigDecimal expectedReturnRate,
        String priority, String status, BigDecimal sipStepUpPct,
        Instant createdAt, Instant updatedAt
) {}
