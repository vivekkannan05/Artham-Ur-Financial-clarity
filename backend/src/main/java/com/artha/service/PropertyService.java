package com.artha.service;

import com.artha.domain.Household;
import com.artha.domain.Liability;
import com.artha.domain.LiabilityStatus;
import com.artha.domain.LiabilityType;
import com.artha.domain.Property;
import com.artha.dto.PropertyRequest;
import com.artha.dto.PropertyResponse;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.LiabilityRepository;
import com.artha.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Property Service — CRUD with computed financial metrics.
 *
 * SPEC REF: FR-6.3 — "rental yield, net equity (current value − mortgage)."
 *
 * COMPUTED FIELDS (not stored, calculated on read):
 *   rentalYield = (monthlyRentalIncome × 12 / currentValue) × 100
 *   netEquity = currentValue - mortgageOutstanding
 *   annualNetIncome = (monthlyRent - monthlyExpenses) × 12
 */
@Service
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final HouseholdRepository householdRepository;
    private final LiabilityRepository liabilityRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           HouseholdRepository householdRepository,
                           LiabilityRepository liabilityRepository) {
        this.propertyRepository = propertyRepository;
        this.householdRepository = householdRepository;
        this.liabilityRepository = liabilityRepository;
    }

    @Transactional
    public PropertyResponse create(UUID householdId, PropertyRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found: " + householdId));

        Property property = new Property();
        property.setHousehold(household);
        mapRequestToEntity(request, property);

        property = propertyRepository.save(property);
        return toResponse(property);
    }

    public List<PropertyResponse> findByHouseholdId(UUID householdId) {
        return propertyRepository.findByHouseholdId(householdId)
                .stream().map(this::toResponse).toList();
    }

    public PropertyResponse findById(UUID id) {
        return toResponse(propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found: " + id)));
    }

    @Transactional
    public PropertyResponse update(UUID id, PropertyRequest request) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found: " + id));
        mapRequestToEntity(request, property);
        property = propertyRepository.save(property);
        return toResponse(property);
    }

    @Transactional
    public void delete(UUID id) {
        if (!propertyRepository.existsById(id)) {
            throw new RuntimeException("Property not found: " + id);
        }
        propertyRepository.deleteById(id);
    }

    // ─── Mapping helpers ─────────────────────────────────────

    private void mapRequestToEntity(PropertyRequest request, Property entity) {
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setPurchaseDate(request.purchaseDate());
        entity.setPurchaseCost(request.purchaseCost());
        entity.setCurrentValue(request.currentValue());
        entity.setValuationDate(request.valuationDate());
        entity.setMonthlyRentalIncome(
                request.monthlyRentalIncome() != null ? request.monthlyRentalIncome() : BigDecimal.ZERO);
        entity.setMonthlyExpenses(
                request.monthlyExpenses() != null ? request.monthlyExpenses() : BigDecimal.ZERO);
        entity.setSelfOccupied(request.selfOccupied() != null ? request.selfOccupied() : true);
        entity.setNotes(request.notes());
    }

    private PropertyResponse toResponse(Property entity) {
        UUID propertyId = entity.getId();
        UUID householdId = entity.getHousehold().getId();

        // Compute rental yield
        BigDecimal rentalYield = BigDecimal.ZERO;
        if (entity.getMonthlyRentalIncome() != null
                && entity.getCurrentValue().compareTo(BigDecimal.ZERO) > 0) {
            rentalYield = entity.getMonthlyRentalIncome()
                    .multiply(BigDecimal.valueOf(12))
                    .divide(entity.getCurrentValue(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Compute net equity: currentValue - linked mortgage outstanding
        BigDecimal mortgageOutstanding = liabilityRepository
                .findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .filter(l -> l.getLiabilityType() == LiabilityType.MORTGAGE)
                .filter(l -> propertyId.equals(l.getPropertyId()))
                .map(Liability::getPrincipalOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netEquity = entity.getCurrentValue()
                .subtract(mortgageOutstanding)
                .setScale(2, RoundingMode.HALF_UP);

        // Annual net income from property
        BigDecimal monthlyNet = entity.getMonthlyRentalIncome()
                .subtract(entity.getMonthlyExpenses());
        BigDecimal annualNetIncome = monthlyNet
                .multiply(BigDecimal.valueOf(12))
                .setScale(2, RoundingMode.HALF_UP);

        return new PropertyResponse(
                entity.getId(),
                householdId,
                entity.getName(),
                entity.getAddress(),
                entity.getPurchaseDate(),
                entity.getPurchaseCost(),
                entity.getCurrentValue(),
                entity.getValuationDate(),
                entity.getMonthlyRentalIncome(),
                entity.getMonthlyExpenses(),
                entity.isSelfOccupied(),
                rentalYield,
                netEquity,
                annualNetIncome,
                entity.getNotes()
        );
    }
}
