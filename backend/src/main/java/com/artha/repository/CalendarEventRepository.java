package com.artha.repository;

import com.artha.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByHouseholdIdOrderByEventDateAsc(UUID householdId);
    List<CalendarEvent> findByHouseholdIdAndEventDateBetweenOrderByEventDateAsc(
            UUID householdId, LocalDate start, LocalDate end);
}
