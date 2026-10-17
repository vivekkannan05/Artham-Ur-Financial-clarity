package com.artha.service;

import com.artha.domain.*;
import com.artha.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Calendar Service — Auto-populates financial events from household data.
 *
 * SPEC REF: FR-7.4 — "Financial calendar with SIP dates, EMI due dates,
 * insurance renewal dates, FD/RD maturity dates, tax filing deadlines,
 * loan rate reset dates, goal review dates, and custom reminders."
 *
 * HOW IT WORKS:
 *   Scans liabilities (EMI dates), insurance policies (renewal dates),
 *   and goals (annual review dates) to create CalendarEvent entries.
 *   This is run on-demand — it doesn't create duplicates if an event
 *   with the same linked entity already exists.
 *
 * EVENTS AUTO-GENERATED:
 *   - EMI:      Monthly, from active liabilities (emiAmount > 0)
 *   - RENEWAL:  Annual, from active insurance policies (premiumDueDate)
 *   - MATURITY: One-time, from liabilities with tenureEnd date
 *   - GOAL_REVIEW: Annual, for each active goal
 *   - TAX_DEADLINE: Fixed dates (July 31 ITR, March 15 advance tax)
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarEventRepository eventRepository;
    private final LiabilityRepository liabilityRepository;
    private final InsurancePolicyRepository insuranceRepository;
    private final GoalRepository goalRepository;
    private final HouseholdRepository householdRepository;

    public CalendarService(CalendarEventRepository eventRepository,
                           LiabilityRepository liabilityRepository,
                           InsurancePolicyRepository insuranceRepository,
                           GoalRepository goalRepository,
                           HouseholdRepository householdRepository) {
        this.eventRepository = eventRepository;
        this.liabilityRepository = liabilityRepository;
        this.insuranceRepository = insuranceRepository;
        this.goalRepository = goalRepository;
        this.householdRepository = householdRepository;
    }

    /**
     * Auto-populate calendar events for a household.
     * Scans data sources and creates events that don't already exist.
     *
     * @return Number of new events created.
     */
    @Transactional
    public int autoPopulate(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        List<CalendarEvent> existingEvents = eventRepository
                .findByHouseholdIdOrderByEventDateAsc(householdId);

        List<CalendarEvent> newEvents = new ArrayList<>();

        newEvents.addAll(generateEmiEvents(household, existingEvents));
        newEvents.addAll(generateRenewalEvents(household, existingEvents));
        newEvents.addAll(generateMaturityEvents(household, existingEvents));
        newEvents.addAll(generateGoalReviewEvents(household, existingEvents));
        newEvents.addAll(generateTaxDeadlines(household, existingEvents));

        eventRepository.saveAll(newEvents);
        return newEvents.size();
    }

    /** EMI due dates — monthly recurring from active liabilities. */
    private List<CalendarEvent> generateEmiEvents(Household household,
                                                   List<CalendarEvent> existing) {
        List<CalendarEvent> events = new ArrayList<>();
        List<Liability> active = liabilityRepository
                .findByHouseholdIdAndStatus(household.getId(), LiabilityStatus.ACTIVE);

        for (Liability l : active) {
            if (alreadyLinked(existing, "LIABILITY", l.getId())) continue;

            // Next EMI date: use the 1st of next month as default
            LocalDate nextEmi = LocalDate.now().withDayOfMonth(1).plusMonths(1);
            CalendarEvent event = new CalendarEvent();
            event.setHousehold(household);
            event.setEventType(CalendarEventType.EMI);
            event.setEventDate(nextEmi);
            event.setRecurrence(Recurrence.MONTHLY);
            event.setDescription(String.format("EMI payment: %s %s — ₹%s",
                    l.getLiabilityType(), l.getLender() != null ? l.getLender() : "",
                    l.getEmiAmount().toPlainString()));
            event.setLinkedEntityType("LIABILITY");
            event.setLinkedEntityId(l.getId());
            events.add(event);
        }
        return events;
    }

    /** Insurance renewal / premium due dates. */
    private List<CalendarEvent> generateRenewalEvents(Household household,
                                                       List<CalendarEvent> existing) {
        List<CalendarEvent> events = new ArrayList<>();
        List<InsurancePolicy> policies = insuranceRepository
                .findByHouseholdIdAndRenewalStatus(household.getId(), RenewalStatus.ACTIVE);

        for (InsurancePolicy p : policies) {
            if (alreadyLinked(existing, "INSURANCE", p.getId())) continue;
            if (p.getPremiumDueDate() == null) continue;

            CalendarEvent event = new CalendarEvent();
            event.setHousehold(household);
            event.setEventType(CalendarEventType.RENEWAL);
            event.setEventDate(p.getPremiumDueDate());
            event.setRecurrence(Recurrence.ANNUAL);
            event.setDescription(String.format("Insurance premium due: %s — ₹%s",
                    p.getPolicyName(),
                    p.getAnnualPremium() != null ? p.getAnnualPremium().toPlainString() : "N/A"));
            event.setLinkedEntityType("INSURANCE");
            event.setLinkedEntityId(p.getId());
            events.add(event);
        }
        return events;
    }

    /** Loan maturity dates (one-time events). */
    private List<CalendarEvent> generateMaturityEvents(Household household,
                                                        List<CalendarEvent> existing) {
        List<CalendarEvent> events = new ArrayList<>();
        List<Liability> all = liabilityRepository.findByHouseholdId(household.getId());

        for (Liability l : all) {
            if (l.getTenureEnd() == null) continue;
            if (l.getTenureEnd().isBefore(LocalDate.now())) continue;
            if (alreadyLinked(existing, "MATURITY_LIABILITY", l.getId())) continue;

            CalendarEvent event = new CalendarEvent();
            event.setHousehold(household);
            event.setEventType(CalendarEventType.MATURITY);
            event.setEventDate(l.getTenureEnd());
            event.setRecurrence(Recurrence.NONE);
            event.setDescription(String.format("Loan maturity: %s %s",
                    l.getLiabilityType(), l.getLender() != null ? l.getLender() : ""));
            event.setLinkedEntityType("MATURITY_LIABILITY");
            event.setLinkedEntityId(l.getId());
            events.add(event);
        }
        return events;
    }

    /** Annual goal review reminders. */
    private List<CalendarEvent> generateGoalReviewEvents(Household household,
                                                          List<CalendarEvent> existing) {
        List<CalendarEvent> events = new ArrayList<>();
        List<Goal> goals = goalRepository
                .findByHouseholdIdAndStatus(household.getId(), GoalStatus.ACTIVE);

        for (Goal g : goals) {
            if (alreadyLinked(existing, "GOAL", g.getId())) continue;

            // Annual review on January 1st
            LocalDate reviewDate = LocalDate.of(LocalDate.now().getYear() + 1, 1, 1);
            CalendarEvent event = new CalendarEvent();
            event.setHousehold(household);
            event.setEventType(CalendarEventType.GOAL_REVIEW);
            event.setEventDate(reviewDate);
            event.setRecurrence(Recurrence.ANNUAL);
            event.setDescription(String.format("Annual review: %s goal", g.getName()));
            event.setLinkedEntityType("GOAL");
            event.setLinkedEntityId(g.getId());
            events.add(event);
        }
        return events;
    }

    /** Fixed tax deadlines — ITR filing (Jul 31) and advance tax (Mar 15). */
    private List<CalendarEvent> generateTaxDeadlines(Household household,
                                                      List<CalendarEvent> existing) {
        List<CalendarEvent> events = new ArrayList<>();
        int year = LocalDate.now().getYear();

        // ITR filing deadline: July 31
        if (!alreadyExists(existing, CalendarEventType.TAX_DEADLINE, "ITR", year)) {
            CalendarEvent itr = new CalendarEvent();
            itr.setHousehold(household);
            itr.setEventType(CalendarEventType.TAX_DEADLINE);
            itr.setEventDate(LocalDate.of(year, 7, 31));
            itr.setRecurrence(Recurrence.ANNUAL);
            itr.setDescription("ITR filing deadline");
            events.add(itr);
        }

        // Advance tax final instalment: March 15
        if (!alreadyExists(existing, CalendarEventType.TAX_DEADLINE, "Advance tax", year)) {
            CalendarEvent advTax = new CalendarEvent();
            advTax.setHousehold(household);
            advTax.setEventType(CalendarEventType.TAX_DEADLINE);
            advTax.setEventDate(LocalDate.of(year, 3, 15));
            advTax.setRecurrence(Recurrence.ANNUAL);
            advTax.setDescription("Advance tax final instalment deadline");
            events.add(advTax);
        }

        return events;
    }

    /** Check if an event with this linked entity already exists. */
    private boolean alreadyLinked(List<CalendarEvent> existing,
                                   String entityType, UUID entityId) {
        return existing.stream().anyMatch(e ->
                entityType.equals(e.getLinkedEntityType())
                        && entityId.equals(e.getLinkedEntityId()));
    }

    /** Check if a specific tax deadline already exists for this year. */
    private boolean alreadyExists(List<CalendarEvent> existing,
                                   CalendarEventType type, String descPrefix, int year) {
        return existing.stream().anyMatch(e ->
                e.getEventType() == type
                        && e.getDescription().startsWith(descPrefix)
                        && e.getEventDate().getYear() == year);
    }
}
