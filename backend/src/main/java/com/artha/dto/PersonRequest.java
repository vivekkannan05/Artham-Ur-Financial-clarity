package com.artha.dto;

import com.artha.domain.Gender;
import com.artha.domain.Relationship;
import com.artha.domain.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request DTO for creating/updating a Person.
 *
 * Note: PAN is accepted as plaintext in the request.
 * The service layer encrypts it before storing (per FR-1.2, S-7).
 */
public record PersonRequest(
        @NotBlank(message = "Person name is required")
        String name,

        @NotNull(message = "Relationship is required")
        Relationship relationship,

        LocalDate dateOfBirth,

        Gender gender,

        /** Plaintext PAN — will be encrypted before storage. */
        String pan,

        TaxRegime taxRegime,

        String residencyStatus,

        String citizenship
) {}
