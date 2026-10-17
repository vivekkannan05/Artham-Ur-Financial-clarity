package com.artha.dto;

import com.artha.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(
        @NotBlank(message = "Institution name is required")
        String institutionName,
        String accountNumberMasked,
        @NotNull(message = "Account type is required")
        AccountType accountType,
        String currency
) {}
