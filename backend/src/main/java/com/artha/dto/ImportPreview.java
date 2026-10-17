package com.artha.dto;

import java.util.List;
import java.util.Map;

/**
 * CSV Import preview — shown to user before committing.
 *
 * SPEC REF: FR-8.2 — "show preview with row count, validation errors,
 * and duplicate warnings."
 */
public record ImportPreview(
        String entityType,         // "TRANSACTION", "HOLDING", etc.
        int totalRows,
        int validRows,
        int errorRows,
        int duplicateRows,
        List<ImportRow> rows,
        List<String> validationErrors,
        List<DuplicateFlag> duplicates
) {
    /**
     * A single row from the import file with its validation status.
     */
    public record ImportRow(
            int rowNumber,
            Map<String, String> data,
            boolean valid,
            String error
    ) {}

    /**
     * A flagged potential duplicate (Algorithm A-13).
     * SPEC REF: EC-14 — "Flag as potential duplicates."
     */
    public record DuplicateFlag(
            int rowNumber,
            Map<String, String> importedData,
            String existingRecordSummary,
            String message
    ) {}
}
