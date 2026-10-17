package com.artha.service;

import com.artha.domain.Goal;
import com.artha.domain.GoalAllocation;
import com.artha.domain.GoalStatus;
import com.artha.domain.Household;
import com.artha.domain.Person;
import com.artha.dto.GoalProjection;
import com.artha.dto.GoalRequest;
import com.artha.dto.GoalResponse;
import com.artha.engine.FundingProjectionEngine;
import com.artha.repository.GoalAllocationRepository;
import com.artha.repository.GoalRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for Goal lifecycle and funding projections.
 * SPEC REFS: FR-4.1 (CRUD), FR-4.2 (projections)
 */
@Service
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final HouseholdRepository householdRepository;
    private final PersonRepository personRepository;
    private final GoalAllocationRepository allocationRepository;

    public GoalService(GoalRepository goalRepository, HouseholdRepository householdRepository,
                       PersonRepository personRepository, GoalAllocationRepository allocationRepository) {
        this.goalRepository = goalRepository;
        this.householdRepository = householdRepository;
        this.personRepository = personRepository;
        this.allocationRepository = allocationRepository;
    }

    public GoalResponse create(UUID householdId, GoalRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        Goal goal = new Goal();
        goal.setHousehold(household);
        goal.setName(request.name());
        goal.setGoalType(request.goalType());
        goal.setTargetDate(request.targetDate());
        goal.setTargetAmountToday(request.targetAmountToday());
        goal.setInflationRate(request.inflationRate());
        goal.setExpectedReturnRate(request.expectedReturnRate());
        goal.setPriority(request.priority());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setSipStepUpPct(request.sipStepUpPct());

        if (request.beneficiaryId() != null) {
            goal.setBeneficiary(personRepository.findById(request.beneficiaryId())
                    .orElseThrow(() -> new RuntimeException("Beneficiary not found")));
        }
        if (request.ownerId() != null) {
            goal.setOwner(personRepository.findById(request.ownerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found")));
        }

        return toResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public GoalResponse findById(UUID id) {
        return toResponse(goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> findByHouseholdId(UUID householdId) {
        return goalRepository.findByHouseholdId(householdId).stream()
                .map(this::toResponse).toList();
    }

    public GoalResponse update(UUID id, GoalRequest request) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + id));
        goal.setName(request.name());
        goal.setGoalType(request.goalType());
        goal.setTargetDate(request.targetDate());
        goal.setTargetAmountToday(request.targetAmountToday());
        goal.setInflationRate(request.inflationRate());
        goal.setExpectedReturnRate(request.expectedReturnRate());
        goal.setPriority(request.priority());
        goal.setSipStepUpPct(request.sipStepUpPct());
        return toResponse(goalRepository.save(goal));
    }

    public void delete(UUID id) {
        if (!goalRepository.existsById(id)) throw new RuntimeException("Goal not found: " + id);
        goalRepository.deleteById(id);
    }

    /**
     * Compute goal funding projection (per FR-4.2).
     */
    @Transactional(readOnly = true)
    public GoalProjection computeProjection(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found: " + goalId));

        int yearsToGoal = 0;
        if (goal.getTargetDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
            yearsToGoal = Math.max(0, (int) (days / 365));
        }

        BigDecimal targetToday = goal.getTargetAmountToday() != null
                ? goal.getTargetAmountToday() : BigDecimal.ZERO;
        BigDecimal inflation = goal.getInflationRate() != null
                ? goal.getInflationRate() : BigDecimal.ZERO;
        BigDecimal expectedReturn = goal.getExpectedReturnRate() != null
                ? goal.getExpectedReturnRate() : new BigDecimal("0.10");
        BigDecimal stepUp = goal.getSipStepUpPct();

        // Inflation-adjusted future target
        BigDecimal inflationTarget = FundingProjectionEngine
                .inflationAdjustedTarget(targetToday, inflation, yearsToGoal);

        // Current corpus allocated to this goal
        BigDecimal currentCorpus = computeAllocatedCorpus(goalId);

        // Projected corpus at target date
        BigDecimal projectedCorpus = FundingProjectionEngine
                .corpusFutureValue(currentCorpus, expectedReturn, yearsToGoal);

        // Funding percentage
        BigDecimal fundingPct = inflationTarget.signum() > 0
                ? projectedCorpus.multiply(BigDecimal.valueOf(100))
                        .divide(inflationTarget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Shortfall (positive) or surplus (negative)
        BigDecimal shortfall = inflationTarget.subtract(projectedCorpus)
                .setScale(2, RoundingMode.HALF_UP);

        // Required monthly SIP to close shortfall
        BigDecimal requiredSip = BigDecimal.ZERO;
        BigDecimal requiredSipStepUp = BigDecimal.ZERO;
        if (shortfall.signum() > 0 && yearsToGoal > 0) {
            requiredSip = FundingProjectionEngine.requiredMonthlySip(shortfall, expectedReturn, yearsToGoal);
            if (stepUp != null && stepUp.signum() > 0) {
                // Approximate: the step-up SIP is smaller because contributions grow
                requiredSipStepUp = requiredSip.multiply(new BigDecimal("0.85"))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        return new GoalProjection(targetToday, inflationTarget, yearsToGoal,
                currentCorpus, projectedCorpus, fundingPct, shortfall,
                requiredSip, requiredSipStepUp);
    }

    private BigDecimal computeAllocatedCorpus(UUID goalId) {
        List<GoalAllocation> allocations = allocationRepository.findByGoalId(goalId);
        BigDecimal total = BigDecimal.ZERO;
        for (GoalAllocation alloc : allocations) {
            BigDecimal holdingValue = alloc.getHolding().getCurrentValue();
            if (holdingValue != null) {
                BigDecimal allocated = holdingValue
                        .multiply(alloc.getAllocationPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                total = total.add(allocated);
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private GoalResponse toResponse(Goal g) {
        return new GoalResponse(g.getId(), g.getHousehold().getId(), g.getName(),
                g.getGoalType().name(),
                g.getBeneficiary() != null ? g.getBeneficiary().getId() : null,
                g.getOwner() != null ? g.getOwner().getId() : null,
                g.getTargetDate(), g.getTargetAmountToday(),
                g.getInflationRate(), g.getExpectedReturnRate(),
                g.getPriority().name(), g.getStatus().name(), g.getSipStepUpPct(),
                g.getCreatedAt(), g.getUpdatedAt());
    }
}
