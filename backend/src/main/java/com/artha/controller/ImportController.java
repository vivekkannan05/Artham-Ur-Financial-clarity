package com.artha.controller;

import com.artha.dto.ImportPreview;
import com.artha.dto.ImportResult;
import com.artha.service.CasPdfParserService;
import com.artha.service.CsvImportService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/**
 * REST controller for data import (CSV + CAS PDF).
 *
 * SPEC REF: FR-8.2 (CSV), FR-8.3 (CAS PDF), FR-8.4 (conflict resolution)
 *
 * FLOW:
 *   1. POST /api/import/csv/preview    → upload CSV, get ImportPreview
 *   2. User reviews preview (duplicates, errors)
 *   3. POST /api/import/csv/commit     → commit approved rows
 *
 *   1. POST /api/import/cas/preview    → upload CAS PDF, get ImportPreview
 *   (Phase 1: returns guidance to use CSV instead)
 */
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final CsvImportService csvImportService;
    private final CasPdfParserService casPdfParserService;

    public ImportController(CsvImportService csvImportService,
                            CasPdfParserService casPdfParserService) {
        this.csvImportService = csvImportService;
        this.casPdfParserService = casPdfParserService;
    }

    /** Download CSV template for a given entity type. */
    @GetMapping("/csv/template/{entityType}")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable String entityType) {
        String filename = entityType.toLowerCase() + "s-template.csv";
        Resource resource = new ClassPathResource("csv-templates/" + filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    /** Upload CSV and get a preview (validation + duplicate detection). */
    @PostMapping("/csv/preview")
    public ResponseEntity<ImportPreview> previewCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("householdId") UUID householdId) throws IOException {

        ImportPreview preview = csvImportService.preview(
                file.getInputStream(), entityType, householdId);
        return ResponseEntity.ok(preview);
    }

    /** Commit a previously previewed CSV import. All-or-nothing (NFR-7.1). */
    @PostMapping("/csv/commit")
    public ResponseEntity<ImportResult> commitCsv(
            @RequestBody ImportPreview preview,
            @RequestParam("householdId") UUID householdId,
            @RequestParam(value = "skipDuplicates", defaultValue = "true") boolean skipDuplicates) {

        ImportResult result = csvImportService.commit(preview, skipDuplicates, householdId);
        return ResponseEntity.ok(result);
    }

    /** Upload CAS PDF for parsing (Phase 1: stub with guidance). */
    @PostMapping("/cas/preview")
    public ResponseEntity<ImportPreview> previewCas(
            @RequestParam("file") MultipartFile file) throws IOException {

        ImportPreview preview = casPdfParserService.parse(file.getInputStream());
        return ResponseEntity.ok(preview);
    }
}
