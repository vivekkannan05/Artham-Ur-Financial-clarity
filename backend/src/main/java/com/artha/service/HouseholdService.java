package com.artha.service;

import com.artha.domain.Household;
import com.artha.domain.HouseholdStatus;
import com.artha.dto.HouseholdRequest;
import com.artha.dto.HouseholdResponse;
import com.artha.dto.PersonResponse;
import com.artha.repository.HouseholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Household lifecycle operations.
 *
 * SPEC REF: FR-1.1 — Household CRUD.
 *
 * WHY @Service?
 *   Marks this class as a Spring-managed bean. Spring creates one instance
 *   and injects it wherever needed (controllers, other services).
 *
 * WHY @Transactional?
 *   Wraps each method in a database transaction. If any DB operation fails,
 *   all changes in that method are rolled back — no partial saves.
 */
@Service
@Transactional
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    /**
     * Constructor injection — Spring automatically provides the repository.
     * This is preferred over @Autowired on fields because:
     *   1. Dependencies are explicit
     *   2. The class can be unit-tested without Spring (just pass mocks)
     *   3. Fields can be final (immutable)
     */
    public HouseholdService(HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    /**
     * Create a new household.
     */
    public HouseholdResponse create(HouseholdRequest request) {
        Household household = new Household();
        household.setName(request.name());
        household.setDescription(request.description());
        household.setStatus(HouseholdStatus.ACTIVE);

        Household saved = householdRepository.save(household);
        return toResponse(saved);
    }

    /**
     * Get a household by ID.
     *
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public HouseholdResponse findById(UUID id) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Household not found: " + id));
        return toResponse(household);
    }

    /**
     * List all households.
     */
    @Transactional(readOnly = true)
    public List<HouseholdResponse> findAll() {
        return householdRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Update a household's name and description.
     */
    public HouseholdResponse update(UUID id, HouseholdRequest request) {
        Household household = householdRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Household not found: " + id));

        household.setName(request.name());
        household.setDescription(request.description());

        Household saved = householdRepository.save(household);
        return toResponse(saved);
    }

    /**
     * Delete a household and all its members (CASCADE).
     */
    public void delete(UUID id) {
        if (!householdRepository.existsById(id)) {
            throw new RuntimeException("Household not found: " + id);
        }
        householdRepository.deleteById(id);
    }

    // ─── Mapping helper ─────────────────────────────────────

    /**
     * Convert entity to response DTO.
     * This is a simple mapper — in larger projects you might use MapStruct.
     * For teaching clarity, we do it manually.
     */
    private HouseholdResponse toResponse(Household household) {
        List<PersonResponse> memberResponses = household.getMembers().stream()
                .map(person -> new PersonResponse(
                        person.getId(),
                        person.getName(),
                        person.getRelationship() != null ? person.getRelationship().name() : null,
                        person.getDateOfBirth(),
                        person.getGender() != null ? person.getGender().name() : null,
                        maskPan(person.getPanEncrypted()),
                        person.getTaxRegime() != null ? person.getTaxRegime().name() : null,
                        person.getResidencyStatus(),
                        person.getCitizenship(),
                        household.getId(),
                        person.getCreatedAt(),
                        person.getUpdatedAt()
                ))
                .toList();

        return new HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getDescription(),
                household.getStatus().name(),
                memberResponses,
                household.getCreatedAt(),
                household.getUpdatedAt()
        );
    }

    /**
     * Mask PAN for API responses.
     * Full PAN is never exposed — only first 4 and last 2 characters shown.
     * Example: "ABCDE1234F" → "ABCD****4F"
     *
     * Note: In production, this would decrypt the encrypted PAN first,
     * then mask it. For now (Phase 1), we store PAN as-is in tests.
     */
    private String maskPan(String panEncrypted) {
        if (panEncrypted == null || panEncrypted.isBlank()) {
            return null;
        }
        // For now, assume panEncrypted is the actual PAN (encryption added later)
        if (panEncrypted.length() < 6) {
            return "****";
        }
        return panEncrypted.substring(0, 4) + "****" + panEncrypted.substring(panEncrypted.length() - 2);
    }
}
