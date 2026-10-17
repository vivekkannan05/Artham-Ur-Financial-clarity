package com.artha.service;

import com.artha.dto.ImportPreview;
import com.artha.dto.ImportPreview.ImportRow;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.*;

/**
 * CAS PDF Parser — Basic parser for CAMS/KFintech Consolidated Account Statements.
 *
 * SPEC REF: FR-8.3 — "Extract folio numbers, fund names, transaction history,
 * units, NAV, and amount. If PDF format is unrecognizable → show error (EC-5)."
 *
 * PHASE 1 LIMITATION:
 *   CAS PDF parsing is complex — CAMS and KFintech formats differ, and
 *   the PDFs are not structured (they're rendered reports, not data exports).
 *   Full implementation requires a PDF library (Apache PDFBox) and regex
 *   patterns for each format variant.
 *
 *   Phase 1 provides the framework (preview/commit pipeline) with a basic
 *   text extraction stub. Full parsing is deferred to Phase 2 when we have
 *   sample CAS PDFs to test against.
 *
 * The service follows the same preview → commit pattern as CsvImportService.
 */
@Service
public class CasPdfParserService {

    /**
     * Parse a CAS PDF and return an import preview.
     *
     * Phase 1: Returns an error indicating that CAS PDF parsing
     * requires additional setup. Users are directed to CSV import or manual entry.
     */
    public ImportPreview parse(InputStream pdfInputStream) {
        // Phase 1: CAS PDF parsing is stubbed.
        // Full implementation with Apache PDFBox comes in Phase 2.
        // Per EC-5: show clear error and suggest alternatives.
        return new ImportPreview(
                "CAS_PDF",
                0, 0, 0, 0,
                List.of(),
                List.of("CAS PDF parsing is available in Phase 2. " +
                        "For now, please use CSV import or manual entry. " +
                        "Export your CAS data as CSV from the AMC website."),
                List.of()
        );
    }
}
