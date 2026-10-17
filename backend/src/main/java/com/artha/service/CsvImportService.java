package com.artha.service;

import com.artha.domain.*;
import com.artha.dto.ImportPreview;
import com.artha.dto.ImportPreview.DuplicateFlag;
import com.artha.dto.ImportPreview.ImportRow;
import com.artha.dto.ImportResult;
import com.artha.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * CSV Import Service — Parse, validate, detect duplicates, commit.
 *
 * SPEC REF: FR-8.2 — "CSV template import per entity type."
 * SPEC REF: NFR-7.1 — "Atomic database transactions (all-or-nothing)."
 *
 * PIPELINE (per FR-8.2):
 *   1. Parse CSV → rows
 *   2. Validate column structure (EC-4: reject if missing required columns)
 *   3. Validate each row (types, required fields)
 *   4. Detect duplicates via HashSet (Algorithm A-13, EC-14)
 *   5. Return ImportPreview for user review
 *   6. On user approval → commit in single @Transactional
 *
 * ALGORITHM A-13 (Design Doc):
 *   Build HashSet of existing records: key = date|amount|instrumentCode|accountId
 *   Check each imported row against the set: O(1) per lookup
 *   Total: O(e + n) where e = existing, n = imported
 */
@Service
public class CsvImportService {

    private static final Set<String> TRANSACTION_REQUIRED_COLUMNS = Set.of(
            "date", "amount", "type", "holding_name"
    );

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;

    public CsvImportService(TransactionRepository transactionRepository,
                            HoldingRepository holdingRepository) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
    }

    /**
     * Preview a CSV import without committing.
     *
     * @param inputStream The CSV file content.
     * @param entityType  Which entity type this CSV contains (e.g., "TRANSACTION").
     * @param householdId The target household.
     * @return ImportPreview with validation results and duplicate flags.
     */
    public ImportPreview preview(InputStream inputStream, String entityType, UUID householdId) {
        List<Map<String, String>> parsedRows = parseCsv(inputStream);

        if (parsedRows.isEmpty()) {
            return new ImportPreview(entityType, 0, 0, 0, 0,
                    List.of(), List.of("CSV file is empty"), List.of());
        }

        // ─── Step 1: Validate column structure (EC-4) ───
        Set<String> columns = parsedRows.get(0).keySet();
        List<String> validationErrors = new ArrayList<>();

        if ("TRANSACTION".equalsIgnoreCase(entityType)) {
            Set<String> missing = new HashSet<>(TRANSACTION_REQUIRED_COLUMNS);
            missing.removeAll(columns);
            if (!missing.isEmpty()) {
                validationErrors.add("Missing required columns: " + missing
                        + ". Download the template for the correct format.");
                return new ImportPreview(entityType, parsedRows.size(), 0,
                        parsedRows.size(), 0, List.of(), validationErrors, List.of());
            }
        }

        // ─── Step 2: Validate each row ───
        List<ImportRow> importRows = new ArrayList<>();
        int validCount = 0;
        int errorCount = 0;

        for (int i = 0; i < parsedRows.size(); i++) {
            Map<String, String> data = parsedRows.get(i);
            String error = validateRow(data, entityType);
            boolean valid = (error == null);
            importRows.add(new ImportRow(i + 1, data, valid, error));
            if (valid) validCount++;
            else errorCount++;
        }

        // ─── Step 3: Duplicate detection (Algorithm A-13) ───
        List<DuplicateFlag> duplicates = detectDuplicates(importRows, householdId, entityType);

        return new ImportPreview(entityType, parsedRows.size(), validCount,
                errorCount, duplicates.size(), importRows, validationErrors, duplicates);
    }

    /**
     * Commit approved rows from a preview. All-or-nothing per NFR-7.1.
     *
     * @param preview     The previewed import.
     * @param skipDuplicates Whether to skip flagged duplicates.
     * @param householdId Target household.
     * @return ImportResult with counts.
     */
    @Transactional
    public ImportResult commit(ImportPreview preview, boolean skipDuplicates, UUID householdId) {
        Set<Integer> duplicateRows = new HashSet<>();
        if (skipDuplicates) {
            preview.duplicates().forEach(d -> duplicateRows.add(d.rowNumber()));
        }

        int imported = 0;
        int skipped = 0;

        for (ImportRow row : preview.rows()) {
            if (!row.valid()) { skipped++; continue; }
            if (duplicateRows.contains(row.rowNumber())) { skipped++; continue; }

            // Commit valid, non-duplicate rows
            if ("TRANSACTION".equalsIgnoreCase(preview.entityType())) {
                commitTransactionRow(row.data(), householdId);
                imported++;
            }
        }

        return new ImportResult(preview.entityType(), imported, skipped,
                String.format("Import complete: %d rows imported, %d skipped", imported, skipped));
    }

    // ─── CSV Parsing ─────────────────────────────────────────

    /**
     * Parse a CSV InputStream into a list of row maps.
     * First row is treated as headers.
     */
    public List<Map<String, String>> parseCsv(InputStream inputStream) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;

            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().toLowerCase();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i], values[i].trim());
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage(), e);
        }
        return rows;
    }

    // ─── Row Validation ──────────────────────────────────────

    private String validateRow(Map<String, String> data, String entityType) {
        if ("TRANSACTION".equalsIgnoreCase(entityType)) {
            return validateTransactionRow(data);
        }
        return null;
    }

    private String validateTransactionRow(Map<String, String> data) {
        String date = data.get("date");
        String amount = data.get("amount");
        String type = data.get("type");

        if (date == null || date.isBlank()) return "Missing date";
        if (amount == null || amount.isBlank()) return "Missing amount";
        if (type == null || type.isBlank()) return "Missing transaction type";

        try { LocalDate.parse(date); }
        catch (DateTimeParseException e) { return "Invalid date format: " + date; }

        try { new BigDecimal(amount); }
        catch (NumberFormatException e) { return "Invalid amount: " + amount; }

        try { TransactionType.valueOf(type.toUpperCase()); }
        catch (IllegalArgumentException e) { return "Invalid transaction type: " + type; }

        return null;
    }

    // ─── Duplicate Detection (Algorithm A-13) ────────────────

    /**
     * Detect potential duplicates by building a HashSet of existing records
     * and checking each imported row.
     *
     * Key = date|amount|holdingName (simplified for Phase 1).
     * Time: O(e + n). Space: O(e).
     */
    public List<DuplicateFlag> detectDuplicates(List<ImportRow> rows, UUID householdId, String entityType) {
        if (!"TRANSACTION".equalsIgnoreCase(entityType)) return List.of();

        // Build hash set of existing transactions
        Set<String> existingKeys = new HashSet<>();
        List<Transaction> existingTxns = transactionRepository
                .findByHoldingAccountOwnerHouseholdId(householdId);
        for (Transaction txn : existingTxns) {
            String key = buildTransactionKey(
                    txn.getTransactionDate().toString(),
                    txn.getAmount().toPlainString(),
                    txn.getHolding() != null ? txn.getHolding().getName() : "");
            existingKeys.add(key);
        }

        // Check imported rows
        List<DuplicateFlag> duplicates = new ArrayList<>();
        for (ImportRow row : rows) {
            if (!row.valid()) continue;
            String key = buildTransactionKey(
                    row.data().getOrDefault("date", ""),
                    row.data().getOrDefault("amount", ""),
                    row.data().getOrDefault("holding_name", ""));
            if (existingKeys.contains(key)) {
                duplicates.add(new DuplicateFlag(
                        row.rowNumber(), row.data(),
                        "Existing record with same date/amount/holding",
                        "Potential duplicate of existing record. Review before importing."
                ));
            }
        }
        return duplicates;
    }

    private String buildTransactionKey(String date, String amount, String holdingName) {
        return date + "|" + amount + "|" + holdingName.toLowerCase();
    }

    // ─── Row Commit ──────────────────────────────────────────

    private void commitTransactionRow(Map<String, String> data, UUID householdId) {
        // Look up or match the holding by name within the household
        String holdingName = data.get("holding_name");
        List<Holding> holdings = holdingRepository.findByOwnerHouseholdId(householdId)
                .stream()
                .filter(h -> h.getName().equalsIgnoreCase(holdingName))
                .toList();

        if (holdings.isEmpty()) {
            throw new RuntimeException("Holding not found: " + holdingName
                    + ". Create the holding first, then import transactions.");
        }

        Holding holding = holdings.get(0);
        Transaction txn = new Transaction();
        txn.setHolding(holding);
        txn.setOwner(holding.getOwner());
        txn.setAccount(holding.getAccount());
        txn.setTransactionDate(LocalDate.parse(data.get("date")));
        txn.setAmount(new BigDecimal(data.get("amount")));
        txn.setTransactionType(TransactionType.valueOf(data.get("type").toUpperCase()));

        if (data.containsKey("units") && !data.get("units").isBlank()) {
            txn.setUnits(new BigDecimal(data.get("units")));
        }
        if (data.containsKey("nav_price") && !data.get("nav_price").isBlank()) {
            txn.setNavPrice(new BigDecimal(data.get("nav_price")));
        }
        if (data.containsKey("notes")) {
            txn.setNotes(data.get("notes"));
        }

        transactionRepository.save(txn);
    }
}
