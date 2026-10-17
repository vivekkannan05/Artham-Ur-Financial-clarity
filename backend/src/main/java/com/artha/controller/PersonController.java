package com.artha.controller;

import com.artha.dto.PersonRequest;
import com.artha.dto.PersonResponse;
import com.artha.service.PersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Person operations.
 *
 * Persons are always nested under a household:
 *   POST   /api/households/{householdId}/persons     → Add a member
 *   GET    /api/households/{householdId}/persons     → List household members
 *
 * Individual person operations by direct ID:
 *   GET    /api/persons/{id}     → Get one person
 *   PUT    /api/persons/{id}     → Update a person
 *   DELETE /api/persons/{id}     → Remove a person
 */
@RestController
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping("/api/households/{householdId}/persons")
    public ResponseEntity<PersonResponse> create(@PathVariable UUID householdId,
                                                  @Valid @RequestBody PersonRequest request) {
        PersonResponse response = personService.create(householdId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/households/{householdId}/persons")
    public ResponseEntity<List<PersonResponse>> findByHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(personService.findByHouseholdId(householdId));
    }

    @GetMapping("/api/persons/{id}")
    public ResponseEntity<PersonResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.findById(id));
    }

    @PutMapping("/api/persons/{id}")
    public ResponseEntity<PersonResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    @DeleteMapping("/api/persons/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
