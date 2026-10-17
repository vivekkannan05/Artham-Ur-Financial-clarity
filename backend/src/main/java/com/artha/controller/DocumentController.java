package com.artha.controller;

import com.artha.domain.Document;
import com.artha.domain.DocumentSensitivity;
import com.artha.repository.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Document management.
 *
 * SPEC REF: FR-7.6 — "Attach document references to any financial entity."
 * SPEC REF: NFR-8.5 — "Sensitive/Confidential documents excluded from list
 * responses unless explicitly requested with includeSensitive=true."
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * List documents for a linked entity.
     * Per NFR-8.5: excludes CONFIDENTIAL by default unless includeSensitive=true.
     */
    @GetMapping
    public ResponseEntity<List<Document>> list(
            @RequestParam String entityType,
            @RequestParam UUID entityId,
            @RequestParam(value = "includeSensitive", defaultValue = "false") boolean includeSensitive) {

        List<Document> docs = documentRepository
                .findByLinkedEntityTypeAndLinkedEntityId(entityType, entityId);

        if (!includeSensitive) {
            docs = docs.stream()
                    .filter(d -> d.getSensitivity() != DocumentSensitivity.CONFIDENTIAL)
                    .toList();
        }
        return ResponseEntity.ok(docs);
    }

    /** Create a document reference. */
    @PostMapping
    public ResponseEntity<Document> create(@RequestBody Document document) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentRepository.save(document));
    }

    /** Update a document reference. */
    @PutMapping("/{id}")
    public ResponseEntity<Document> update(@PathVariable UUID id, @RequestBody Document body) {
        Document existing = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        existing.setFileName(body.getFileName());
        existing.setFilePath(body.getFilePath());
        existing.setDocumentType(body.getDocumentType());
        existing.setDocumentDate(body.getDocumentDate());
        existing.setSensitivity(body.getSensitivity());
        existing.setSource(body.getSource());
        return ResponseEntity.ok(documentRepository.save(existing));
    }

    /** Delete a document reference. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
