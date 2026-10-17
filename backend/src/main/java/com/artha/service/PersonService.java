package com.artha.service;

import com.artha.domain.Household;
import com.artha.domain.Person;
import com.artha.dto.PersonRequest;
import com.artha.dto.PersonResponse;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Person lifecycle operations.
 *
 * SPEC REFS: FR-1.1 (person belongs to household), FR-1.2 (person data model).
 */
@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final HouseholdRepository householdRepository;

    public PersonService(PersonRepository personRepository,
                         HouseholdRepository householdRepository) {
        this.personRepository = personRepository;
        this.householdRepository = householdRepository;
    }

    /**
     * Add a new person to a household.
     */
    public PersonResponse create(UUID householdId, PersonRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        Person person = new Person();
        person.setName(request.name());
        person.setRelationship(request.relationship());
        person.setDateOfBirth(request.dateOfBirth());
        person.setGender(request.gender());
        person.setTaxRegime(request.taxRegime());
        person.setResidencyStatus(request.residencyStatus());
        person.setCitizenship(request.citizenship());
        person.setHousehold(household);

        // Store PAN — in production, this would be encrypted first (per FR-1.2, S-7)
        if (request.pan() != null && !request.pan().isBlank()) {
            person.setPanEncrypted(request.pan()); // TODO: Encrypt with AES-256
        }

        Person saved = personRepository.save(person);
        return toResponse(saved);
    }

    /**
     * Get a person by ID.
     */
    @Transactional(readOnly = true)
    public PersonResponse findById(UUID id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found: " + id));
        return toResponse(person);
    }

    /**
     * List all members of a household.
     */
    @Transactional(readOnly = true)
    public List<PersonResponse> findByHouseholdId(UUID householdId) {
        return personRepository.findByHouseholdId(householdId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Update a person's details.
     */
    public PersonResponse update(UUID id, PersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found: " + id));

        person.setName(request.name());
        person.setRelationship(request.relationship());
        person.setDateOfBirth(request.dateOfBirth());
        person.setGender(request.gender());
        person.setTaxRegime(request.taxRegime());
        person.setResidencyStatus(request.residencyStatus());
        person.setCitizenship(request.citizenship());

        if (request.pan() != null && !request.pan().isBlank()) {
            person.setPanEncrypted(request.pan()); // TODO: Encrypt with AES-256
        }

        Person saved = personRepository.save(person);
        return toResponse(saved);
    }

    /**
     * Delete a person.
     */
    public void delete(UUID id) {
        if (!personRepository.existsById(id)) {
            throw new RuntimeException("Person not found: " + id);
        }
        personRepository.deleteById(id);
    }

    // ─── Mapping ─────────────────────────────────────────────

    private PersonResponse toResponse(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getName(),
                person.getRelationship() != null ? person.getRelationship().name() : null,
                person.getDateOfBirth(),
                person.getGender() != null ? person.getGender().name() : null,
                maskPan(person.getPanEncrypted()),
                person.getTaxRegime() != null ? person.getTaxRegime().name() : null,
                person.getResidencyStatus(),
                person.getCitizenship(),
                person.getHousehold().getId(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }

    private String maskPan(String panEncrypted) {
        if (panEncrypted == null || panEncrypted.isBlank()) {
            return null;
        }
        if (panEncrypted.length() < 6) {
            return "****";
        }
        return panEncrypted.substring(0, 4) + "****" + panEncrypted.substring(panEncrypted.length() - 2);
    }
}
