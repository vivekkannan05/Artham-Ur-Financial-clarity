package com.artha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

/**
 * BaseEntity — Shared superclass for ALL Artha entities (per AD-2 in Design Doc).
 *
 * WHY a superclass?
 *   Every table in Artha has these 3 columns: id, created_at, updated_at.
 *   Instead of duplicating this in every entity, we define it once here.
 *   All entities extend BaseEntity.
 *
 * KEY CONCEPTS:
 *   - @MappedSuperclass: Tells JPA "this class provides columns to subclasses,
 *     but it's NOT a table itself." There is no 'base_entity' table.
 *   - UUID primary keys: Globally unique, no sequence conflicts, safe for distributed systems.
 *   - @PrePersist / @PreUpdate: JPA lifecycle hooks — automatically set timestamps
 *     when an entity is first saved or later updated.
 *
 * SPEC REFS: AD-2 (BaseEntity superclass), NFR-2.1 (audit trail timestamps)
 */
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * Primary key — UUID v4, generated automatically.
     * UUID is preferred over auto-increment because:
     *   1. No DB sequence coordination needed
     *   2. Safe for future distributed/sharded scenarios
     *   3. Not guessable (better for API URLs)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * When this record was first created. Set once, never changed.
     * Used for audit trail (NFR-2.1).
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * When this record was last modified. Updated on every save.
     * Used for audit trail (NFR-2.1) and stale-data detection (NFR-2.2).
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA lifecycle hook: called automatically BEFORE first INSERT.
     * Sets both timestamps to "now."
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA lifecycle hook: called automatically BEFORE every UPDATE.
     * Updates the "last modified" timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ─── Getters & Setters ───────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
