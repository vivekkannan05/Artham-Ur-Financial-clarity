package com.artha.service;

import com.artha.domain.Household;
import com.artha.domain.Liability;
import com.artha.domain.LiabilityStatus;
import com.artha.domain.LiabilityType;
import com.artha.dto.LiabilityRequest;
import com.artha.dto.LiabilityResponse;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.LiabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Liability Service — CRUD operations for household debts.
 *
 * SPEC REF: FR-5.1 — "Model liabilities with: type, lender, principal outstanding,
 * interest rate, EMI amount, tenure, prepayment history."
 *
 * RELATIONSHIP TO CASH FLOW:
 *   When liabilities are created/updated, the CashFlowService can now compute
 *   the debt-service ratio: totalEMIs / grossIncome. This was 0 in Batch 1
 *   (no liabilities existed). Now it's live.
 *
 * RELATIONSHIP TO NET WORTH:
 *   Active liabilities' principalOutstanding values are summed as the
 *   "liabilities side" of the net-worth equation.
 */
@Service
@Transactional(readOnly = true)
public class LiabilityService {

    private final LiabilityRepository liabilityRepository;
    private final HouseholdRepository householdRepository;

    public LiabilityService(LiabilityRepository liabilityRepository,
                            HouseholdRepository householdRepository) {
        this.liabilityRepository = liabilityRepository;
        this.householdRepository = householdRepository;
    }

    @Transactional
    public LiabilityResponse create(UUID householdId, LiabilityRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        Liability liability = new Liability();
        liability.setHousehold(household);
        mapRequestToEntity(request, liability);

        liability = liabilityRepository.save(liability);
        return toResponse(liability);
    }

    public List<LiabilityResponse> findByHouseholdId(UUID householdId) {
        return liabilityRepository.findByHouseholdId(householdId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LiabilityResponse findById(UUID id) {
        Liability liability = liabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Liability not found: " + id));
        return toResponse(liability);
    }

    @Transactional
    public LiabilityResponse update(UUID id, LiabilityRequest request) {
        Liability liability = liabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Liability not found: " + id));

        mapRequestToEntity(request, liability);
        liability = liabilityRepository.save(liability);
        return toResponse(liability);
    }

    @Transactional
    public void delete(UUID id) {
        if (!liabilityRepository.existsById(id)) {
            throw new RuntimeException("Liability not found: " + id);
        }
        liabilityRepository.deleteById(id);
    }

    /**
     * Sum all active EMIs for a household.
     * Used by CashFlowService for debt-service ratio calculation.
     *
     * @return Total monthly EMI across all active liabilities.
     */
    public BigDecimal totalActiveEmi(UUID householdId) {
        return liabilityRepository.findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .map(Liability::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Mapping helpers ─────────────────────────────────────

    private void mapRequestToEntity(LiabilityRequest request, Liability entity) {
        entity.setLiabilityType(LiabilityType.valueOf(request.liabilityType()));
        entity.setLender(request.lender());
        entity.setPrincipalOutstanding(request.principalOutstanding());
        entity.setOriginalPrincipal(request.originalPrincipal());
        entity.setInterestRate(request.interestRate());
        entity.setEmiAmount(request.emiAmount());
        entity.setTenureStart(request.tenureStart());
        entity.setTenureEnd(request.tenureEnd());
        entity.setPrepaymentTotal(
                request.prepaymentTotal() != null ? request.prepaymentTotal() : BigDecimal.ZERO);
        entity.setPropertyId(request.propertyId());
        entity.setNotes(request.notes());
    }

    private LiabilityResponse toResponse(Liability entity) {
        return new LiabilityResponse(
                entity.getId(),
                entity.getHousehold().getId(),
                entity.getLiabilityType().name(),
                entity.getStatus().name(),
                entity.getLender(),
                entity.getPrincipalOutstanding(),
                entity.getOriginalPrincipal(),
                entity.getInterestRate(),
                entity.getEmiAmount(),
                entity.getTenureStart(),
                entity.getTenureEnd(),
                entity.getPrepaymentTotal(),
                entity.getPropertyId(),
                entity.getNotes()
        );
    }
}
