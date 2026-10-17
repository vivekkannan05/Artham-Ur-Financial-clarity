package com.artha.controller;

import com.artha.domain.CalendarEvent;
import com.artha.domain.CalendarEventType;
import com.artha.domain.Household;
import com.artha.domain.Recurrence;
import com.artha.repository.CalendarEventRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.service.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** REST controller for Financial Calendar. SPEC REF: FR-7.4 */
@RestController
public class CalendarController {

    private final CalendarEventRepository eventRepository;
    private final HouseholdRepository householdRepository;
    private final CalendarService calendarService;

    public CalendarController(CalendarEventRepository eventRepository,
                              HouseholdRepository householdRepository,
                              CalendarService calendarService) {
        this.eventRepository = eventRepository;
        this.householdRepository = householdRepository;
        this.calendarService = calendarService;
    }

    @PostMapping("/api/households/{householdId}/calendar")
    public ResponseEntity<Map<String, Object>> create(@PathVariable UUID householdId,
                                                       @RequestBody Map<String, String> body) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        CalendarEvent event = new CalendarEvent();
        event.setHousehold(household);
        event.setEventType(CalendarEventType.valueOf(body.get("eventType")));
        event.setEventDate(LocalDate.parse(body.get("eventDate")));
        event.setRecurrence(body.containsKey("recurrence")
                ? Recurrence.valueOf(body.get("recurrence")) : Recurrence.NONE);
        event.setDescription(body.get("description"));
        event.setLinkedEntityType(body.get("linkedEntityType"));
        if (body.containsKey("linkedEntityId")) {
            event.setLinkedEntityId(UUID.fromString(body.get("linkedEntityId")));
        }

        event = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", event.getId(),
                "eventType", event.getEventType().name(),
                "eventDate", event.getEventDate().toString(),
                "description", event.getDescription()
        ));
    }

    @GetMapping("/api/households/{householdId}/calendar")
    public ResponseEntity<List<CalendarEvent>> getAll(@PathVariable UUID householdId) {
        return ResponseEntity.ok(
                eventRepository.findByHouseholdIdOrderByEventDateAsc(householdId));
    }

    @GetMapping("/api/households/{householdId}/calendar/range")
    public ResponseEntity<List<CalendarEvent>> getRange(
            @PathVariable UUID householdId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(
                eventRepository.findByHouseholdIdAndEventDateBetweenOrderByEventDateAsc(
                        householdId, start, end));
    }

    @DeleteMapping("/api/calendar/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Auto-populate calendar from SIP/EMI/renewal/goal data (FR-7.4). */
    @PostMapping("/api/households/{householdId}/calendar/auto-populate")
    public ResponseEntity<Map<String, Object>> autoPopulate(@PathVariable UUID householdId) {
        int created = calendarService.autoPopulate(householdId);
        return ResponseEntity.ok(Map.of(
                "eventsCreated", created,
                "message", String.format("Auto-populated %d calendar events", created)
        ));
    }
}
