package com.artha.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Household — The top-level entity in Artha.
 *
 * A household represents a family unit: one primary user + spouse + children + dependents.
 * ALL financial data (accounts, goals, liabilities, recommendations) belongs to a household.
 *
 * SPEC REF: FR-1.1 — "The system SHALL allow creating and editing a household
 * with one or more members."
 *
 * DESIGN NOTE: In Phase 1, there's only one household (single-user, no auth).
 * But the data model supports multiple households for future extensibility.
 *
 * RELATIONSHIPS:
 *   Household → many Person (members)
 *   Household → many Goal, Liability, Property, Recommendation, etc. (added in later batches)
 */
@Entity
@Table(name = "household")
public class Household extends BaseEntity {

    /**
     * Display name for the household (e.g., "The Sharma Family").
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Optional description or notes about this household.
     */
    @Column(name = "description")
    private String description;

    /**
     * Household status — ACTIVE or ARCHIVED.
     * Archived households are retained for historical data but hidden from the main UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HouseholdStatus status = HouseholdStatus.ACTIVE;

    /**
     * Members of this household (persons).
     *
     * CascadeType.ALL: When a household is saved/deleted, all its members are too.
     * orphanRemoval: If a person is removed from this list, it's deleted from the DB.
     *
     * mappedBy: The 'household' field on Person owns the FK column.
     */
    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Person> members = new ArrayList<>();

    // ─── Getters & Setters ───────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HouseholdStatus getStatus() {
        return status;
    }

    public void setStatus(HouseholdStatus status) {
        this.status = status;
    }

    public List<Person> getMembers() {
        return members;
    }

    public void setMembers(List<Person> members) {
        this.members = members;
    }

    /**
     * Convenience method to add a member and keep the bidirectional relationship in sync.
     */
    public void addMember(Person person) {
        members.add(person);
        person.setHousehold(this);
    }

    /**
     * Convenience method to remove a member.
     */
    public void removeMember(Person person) {
        members.remove(person);
        person.setHousehold(null);
    }
}
