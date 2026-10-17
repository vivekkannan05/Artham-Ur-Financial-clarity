package com.artha.dto;

import com.artha.domain.GoalPriority;
import com.artha.domain.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalRequest(
        @NotBlank(message = "Goal name is required") String name,
        @NotNull(message = "Goal type is required") GoalType goalType,
        UUID beneficiaryId,
        UUID ownerId,
        LocalDate targetDate,
        BigDecimal targetAmountToday,
        BigDecimal inflationRate,
        BigDecimal expectedReturnRate,
        @NotNull(message = "Priority is required") GoalPriority priority,
        BigDecimal sipStepUpPct
) {}
