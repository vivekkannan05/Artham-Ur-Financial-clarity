package com.artha.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/** Calendar Event — A financial milestone or reminder. SPEC REF: FR-7.4 */
@Entity
@Table(name = "calendar_event")
public class CalendarEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CalendarEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence", nullable = false)
    private Recurrence recurrence = Recurrence.NONE;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "linked_entity_type", length = 50)
    private String linkedEntityType;

    @Column(name = "linked_entity_id")
    private UUID linkedEntityId;

    // ─── Getters & Setters ───────────────────────────────────
    public Household getHousehold() { return household; }
    public void setHousehold(Household household) { this.household = household; }
    public CalendarEventType getEventType() { return eventType; }
    public void setEventType(CalendarEventType eventType) { this.eventType = eventType; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLinkedEntityType() { return linkedEntityType; }
    public void setLinkedEntityType(String linkedEntityType) { this.linkedEntityType = linkedEntityType; }
    public UUID getLinkedEntityId() { return linkedEntityId; }
    public void setLinkedEntityId(UUID linkedEntityId) { this.linkedEntityId = linkedEntityId; }
}
