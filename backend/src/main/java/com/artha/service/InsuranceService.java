package com.artha.service;

import com.artha.domain.Frequency;
import com.artha.domain.Household;
import com.artha.domain.Income;
import com.artha.domain.InsurancePolicy;
import com.artha.domain.InsurancePolicyType;
import com.artha.domain.Person;
import com.artha.domain.RenewalStatus;
import com.artha.dto.InsuranceCoverageGap;
import com.artha.dto.InsuranceCoverageGap.CoverageAnalysis;
import com.artha.dto.InsurancePolicyRequest;
import com.artha.dto.InsurancePolicyResponse;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.IncomeRepository;
import com.artha.repository.InsurancePolicyRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Insurance Service — CRUD + coverage gap analysis.
 *
 * SPEC REF: FR-6.2 — Insurance management with basic gap detection.
 *
 * COVERAGE GAP LOGIC:
 *   (a) Term life: sum of ACTIVE term life policies' sumAssured for a person.
 *       If < TERM_LIFE_MULTIPLIER × person's annual income → flag.
 *   (b) Health: sum of ACTIVE health policies' sumAssured for the household.
 *       If < HEALTH_COVER_MIN → flag.
 *
 * SPEC-BINDING: TERM_LIFE_MULTIPLIER=10, HEALTH_COVER_MIN=1000000
 */
@Service
@Transactional(readOnly = true)
public class InsuranceService {

    /** Coverage gap threshold: term life cover must be >= 10× annual income (FR-6.2). */
    private static final int TERM_LIFE_MULTIPLIER = 10;  // SPEC-BINDING FR-6.2

    /** Minimum health cover per family: ₹10,00,000 (FR-6.2). */
    private static final BigDecimal HEALTH_COVER_MIN = new BigDecimal("1000000");  // SPEC-BINDING FR-6.2

    private final InsurancePolicyRepository policyRepository;
    private final HouseholdRepository householdRepository;
    private final PersonRepository personRepository;
    private final IncomeRepository incomeRepository;

    public InsuranceService(InsurancePolicyRepository policyRepository,
                            HouseholdRepository householdRepository,
                            PersonRepository personRepository,
                            IncomeRepository incomeRepository) {
        this.policyRepository = policyRepository;
        this.householdRepository = householdRepository;
        this.personRepository = personRepository;
        this.incomeRepository = incomeRepository;
    }

    @Transactional
    public InsurancePolicyResponse create(UUID householdId, InsurancePolicyRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));
        Person insured = personRepository.findById(request.insuredPersonId())
                .orElseThrow(() -> new RuntimeException("Insured person not found: " + request.insuredPersonId()));
        Person owner = personRepository.findById(request.ownerId())
                .orElseThrow(() -> new RuntimeException("Policy owner not found: " + request.ownerId()));

        InsurancePolicy policy = new InsurancePolicy();
        policy.setHousehold(household);
        policy.setInsuredPerson(insured);
        policy.setOwner(owner);
        mapRequestToEntity(request, policy);

        policy = policyRepository.save(policy);
        return toResponse(policy);
    }

    public List<InsurancePolicyResponse> findByHouseholdId(UUID householdId) {
        return policyRepository.findByHouseholdId(householdId)
                .stream().map(this::toResponse).toList();
    }

    public InsurancePolicyResponse findById(UUID id) {
        return toResponse(policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance policy not found: " + id)));
    }

    @Transactional
    public InsurancePolicyResponse update(UUID id, InsurancePolicyRequest request) {
        InsurancePolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance policy not found: " + id));

        Person insured = personRepository.findById(request.insuredPersonId())
                .orElseThrow(() -> new RuntimeException("Insured person not found: " + request.insuredPersonId()));
        Person owner = personRepository.findById(request.ownerId())
                .orElseThrow(() -> new RuntimeException("Policy owner not found: " + request.ownerId()));

        policy.setInsuredPerson(insured);
        policy.setOwner(owner);
        mapRequestToEntity(request, policy);

        policy = policyRepository.save(policy);
        return toResponse(policy);
    }

    @Transactional
    public void delete(UUID id) {
        if (!policyRepository.existsById(id)) {
            throw new RuntimeException("Insurance policy not found: " + id);
        }
        policyRepository.deleteById(id);
    }

    /**
     * Analyze coverage gaps for a household (FR-6.2).
     *
     * Checks:
     *   1. Per earning member: term life cover vs 10× annual income
     *   2. Household-wide: total health cover vs ₹10L minimum
     */
    public CoverageAnalysis analyzeCoverageGaps(UUID householdId) {
        List<InsuranceCoverageGap> gaps = new ArrayList<>();

        List<InsurancePolicy> activePolicies = policyRepository
                .findByHouseholdIdAndRenewalStatus(householdId, RenewalStatus.ACTIVE);

        // ─── Check 1: Term life per earning member ───
        List<Person> members = personRepository.findByHouseholdId(householdId);
        for (Person person : members) {
            BigDecimal annualIncome = incomeRepository.findByPersonId(person.getId())
                    .stream()
                    .map(i -> normalizeToAnnual(i.getAmount(), i.getFrequency()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (annualIncome.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal recommended = annualIncome.multiply(BigDecimal.valueOf(TERM_LIFE_MULTIPLIER));
            BigDecimal actual = activePolicies.stream()
                    .filter(p -> p.getPolicyType() == InsurancePolicyType.TERM_LIFE)
                    .filter(p -> p.getInsuredPerson().getId().equals(person.getId()))
                    .map(InsurancePolicy::getSumAssured)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (actual.compareTo(recommended) < 0) {
                gaps.add(new InsuranceCoverageGap(
                        "TERM_LIFE",
                        person.getName(),
                        recommended.setScale(2, RoundingMode.HALF_UP),
                        actual.setScale(2, RoundingMode.HALF_UP),
                        recommended.subtract(actual).setScale(2, RoundingMode.HALF_UP),
                        String.format("Term life cover for %s is ₹%.0f — recommended minimum is ₹%.0f (10× annual income).",
                                person.getName(), actual, recommended)
                ));
            }
        }

        // ─── Check 2: Health cover for household ───
        BigDecimal totalHealth = activePolicies.stream()
                .filter(p -> p.getPolicyType() == InsurancePolicyType.HEALTH)
                .map(InsurancePolicy::getSumAssured)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalHealth.compareTo(HEALTH_COVER_MIN) < 0) {
            gaps.add(new InsuranceCoverageGap(
                    "HEALTH",
                    "Household",
                    HEALTH_COVER_MIN.setScale(2, RoundingMode.HALF_UP),
                    totalHealth.setScale(2, RoundingMode.HALF_UP),
                    HEALTH_COVER_MIN.subtract(totalHealth).setScale(2, RoundingMode.HALF_UP),
                    String.format("Total health cover is ₹%.0f — recommended minimum is ₹10,00,000.", totalHealth)
            ));
        }

        return new CoverageAnalysis(gaps, !gaps.isEmpty());
    }

    // ─── Mapping helpers ─────────────────────────────────────

    private void mapRequestToEntity(InsurancePolicyRequest request, InsurancePolicy entity) {
        entity.setPolicyType(InsurancePolicyType.valueOf(request.policyType()));
        entity.setPolicyName(request.policyName());
        entity.setInsurer(request.insurer());
        entity.setPolicyNumber(request.policyNumber());
        entity.setSumAssured(request.sumAssured());
        entity.setAnnualPremium(request.annualPremium());
        entity.setPremiumDueDate(request.premiumDueDate());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setRenewalStatus(request.renewalStatus() != null
                ? RenewalStatus.valueOf(request.renewalStatus())
                : RenewalStatus.ACTIVE);
        entity.setNotes(request.notes());
    }

    private InsurancePolicyResponse toResponse(InsurancePolicy entity) {
        return new InsurancePolicyResponse(
                entity.getId(),
                entity.getHousehold().getId(),
                entity.getPolicyType().name(),
                entity.getInsuredPerson().getId(),
                entity.getInsuredPerson().getName(),
                entity.getOwner().getId(),
                entity.getOwner().getName(),
                entity.getPolicyName(),
                entity.getInsurer(),
                entity.getPolicyNumber(),
                entity.getSumAssured(),
                entity.getAnnualPremium(),
                entity.getPremiumDueDate(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getRenewalStatus().name(),
                entity.getNotes()
        );
    }

    /** Normalize any frequency to annual equivalent. */
    private BigDecimal normalizeToAnnual(BigDecimal amount, Frequency frequency) {
        return switch (frequency) {
            case MONTHLY -> amount.multiply(BigDecimal.valueOf(12));
            case QUARTERLY -> amount.multiply(BigDecimal.valueOf(4));
            case SEMI_ANNUAL -> amount.multiply(BigDecimal.valueOf(2));
            case ANNUAL -> amount;
            case ONE_TIME -> amount;  // One-time income treated as annual for gap analysis
        };
    }
}
