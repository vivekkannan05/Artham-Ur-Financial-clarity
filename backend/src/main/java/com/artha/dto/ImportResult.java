package com.artha.dto;

/**
 * Result of committing an import batch.
 * SPEC REF: FR-8.2 — "commit approved rows in a single database transaction."
 */
public record ImportResult(
        String entityType,
        int rowsImported,
        int rowsSkipped,
        String message
) {}
