package com.artha.controller;

import com.artha.domain.InstrumentType;
import com.artha.repository.InstrumentTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST Controller for the Instrument Type catalog.
 *
 * Read-only — types are seeded via Flyway migration and can be added via DB insert.
 * The frontend uses this to populate the instrument type dropdown.
 *
 * SPEC REF: FR-3.1
 */
@RestController
@RequestMapping("/api/instrument-types")
public class InstrumentTypeController {

    private final InstrumentTypeRepository repository;

    public InstrumentTypeController(InstrumentTypeRepository repository) {
        this.repository = repository;
    }

    /** List all instrument types (for dropdown). */
    @GetMapping
    public ResponseEntity<List<InstrumentType>> findAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    /** Get one instrument type by code. */
    @GetMapping("/{code}")
    public ResponseEntity<InstrumentType> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(repository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Instrument type not found: " + code)));
    }
}
