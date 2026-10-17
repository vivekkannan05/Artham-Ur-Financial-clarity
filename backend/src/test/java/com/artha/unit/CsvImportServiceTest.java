package com.artha.unit;

import com.artha.dto.ImportPreview;
import com.artha.dto.ImportPreview.DuplicateFlag;
import com.artha.dto.ImportPreview.ImportRow;
import com.artha.repository.HoldingRepository;
import com.artha.repository.TransactionRepository;
import com.artha.service.CsvImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CsvImportService: parsing, validation, duplicate detection.
 *
 * SPEC REFS: FR-8.2, EC-4 (missing columns), EC-14 (duplicates), A-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FR-8.2: CSV Import Pipeline")
class CsvImportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private HoldingRepository holdingRepository;

    private CsvImportService service;
    private final UUID householdId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CsvImportService(transactionRepository, holdingRepository);
    }

    @Test
    @DisplayName("Parse valid CSV with 3 rows")
    void testParseCsv() {
        String csv = "date,amount,type,holding_name\n"
                + "2026-01-15,10000.00,SIP,Axis Bluechip\n"
                + "2026-02-15,10000.00,SIP,Axis Bluechip\n"
                + "2026-03-15,10000.00,SIP,Axis Bluechip\n";

        List<Map<String, String>> rows = service.parseCsv(toStream(csv));

        assertEquals(3, rows.size());
        assertEquals("2026-01-15", rows.get(0).get("date"));
        assertEquals("10000.00", rows.get(0).get("amount"));
    }

    @Test
    @DisplayName("EC-4: Missing required columns → reject with error")
    void testMissingColumns() {
        String csv = "date,amount\n2026-01-15,10000.00\n";

        // No mock setup needed: method returns early before reaching duplicate detection
        ImportPreview preview = service.preview(toStream(csv), "TRANSACTION", householdId);

        assertFalse(preview.validationErrors().isEmpty());
        assertTrue(preview.validationErrors().get(0).contains("Missing required columns"));
        assertEquals(0, preview.validRows());
    }

    @Test
    @DisplayName("Row validation: invalid date format → error")
    void testInvalidDate() {
        String csv = "date,amount,type,holding_name\n"
                + "15-01-2026,10000.00,SIP,Axis\n";

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of());

        ImportPreview preview = service.preview(toStream(csv), "TRANSACTION", householdId);

        assertEquals(1, preview.errorRows());
        assertFalse(preview.rows().get(0).valid());
        assertTrue(preview.rows().get(0).error().contains("Invalid date"));
    }

    @Test
    @DisplayName("Row validation: invalid transaction type → error")
    void testInvalidTransactionType() {
        String csv = "date,amount,type,holding_name\n"
                + "2026-01-15,10000.00,INVALID_TYPE,Axis\n";

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of());

        ImportPreview preview = service.preview(toStream(csv), "TRANSACTION", householdId);

        assertEquals(1, preview.errorRows());
        assertTrue(preview.rows().get(0).error().contains("Invalid transaction type"));
    }

    @Test
    @DisplayName("A-13: Duplicate detection via HashSet")
    void testDuplicateDetection() {
        // Build import rows
        ImportRow row1 = new ImportRow(1,
                Map.of("date", "2026-01-15", "amount", "10000.00",
                        "type", "SIP", "holding_name", "Axis Bluechip"),
                true, null);
        ImportRow row2 = new ImportRow(2,
                Map.of("date", "2026-02-15", "amount", "10000.00",
                        "type", "SIP", "holding_name", "Axis Bluechip"),
                true, null);

        // Simulate existing transaction that matches row1
        com.artha.domain.Transaction existing = new com.artha.domain.Transaction();
        existing.setTransactionDate(java.time.LocalDate.of(2026, 1, 15));
        existing.setAmount(new java.math.BigDecimal("10000.00"));
        com.artha.domain.Holding mockHolding = new com.artha.domain.Holding();
        mockHolding.setName("Axis Bluechip");
        existing.setHolding(mockHolding);

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of(existing));

        List<DuplicateFlag> duplicates = service.detectDuplicates(
                List.of(row1, row2), householdId, "TRANSACTION");

        assertEquals(1, duplicates.size(), "Only row1 should be flagged as duplicate");
        assertEquals(1, duplicates.get(0).rowNumber());
    }

    @Test
    @DisplayName("A-13: No duplicates when database is empty")
    void testNoDuplicatesEmptyDb() {
        ImportRow row = new ImportRow(1,
                Map.of("date", "2026-01-15", "amount", "10000.00",
                        "type", "SIP", "holding_name", "Axis"),
                true, null);

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of());

        List<DuplicateFlag> duplicates = service.detectDuplicates(
                List.of(row), householdId, "TRANSACTION");

        assertTrue(duplicates.isEmpty());
    }

    @Test
    @DisplayName("Empty CSV → zero rows")
    void testEmptyCsv() {
        ImportPreview preview = service.preview(toStream(""), "TRANSACTION", householdId);
        assertEquals(0, preview.totalRows());
        assertTrue(preview.validationErrors().get(0).contains("empty"));
    }

    @Test
    @DisplayName("Valid preview with mixed valid/invalid rows")
    void testMixedRows() {
        String csv = "date,amount,type,holding_name\n"
                + "2026-01-15,10000.00,SIP,Axis\n"
                + "bad-date,10000.00,SIP,Axis\n"
                + "2026-03-15,,SIP,Axis\n";

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of());

        ImportPreview preview = service.preview(toStream(csv), "TRANSACTION", householdId);

        assertEquals(3, preview.totalRows());
        assertEquals(1, preview.validRows());
        assertEquals(2, preview.errorRows());
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
