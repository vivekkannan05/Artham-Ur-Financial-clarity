package com.artha.service;

import com.artha.domain.Account;
import com.artha.domain.Holding;
import com.artha.domain.HoldingStatus;
import com.artha.domain.InstrumentType;
import com.artha.domain.Person;
import com.artha.dto.HoldingRequest;
import com.artha.dto.HoldingResponse;
import com.artha.repository.AccountRepository;
import com.artha.repository.HoldingRepository;
import com.artha.repository.InstrumentTypeRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Holding lifecycle operations.
 * SPEC REF: FR-3.3, EC-10 (closed holding retained for history)
 */
@Service
@Transactional
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final InstrumentTypeRepository instrumentTypeRepository;

    public HoldingService(HoldingRepository holdingRepository, AccountRepository accountRepository,
                          PersonRepository personRepository, InstrumentTypeRepository instrumentTypeRepository) {
        this.holdingRepository = holdingRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.instrumentTypeRepository = instrumentTypeRepository;
    }

    public HoldingResponse create(UUID personId, HoldingRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.accountId()));
        InstrumentType instrumentType = instrumentTypeRepository.findByCode(request.instrumentTypeCode())
                .orElseThrow(() -> new RuntimeException("Instrument type not found: " + request.instrumentTypeCode()));

        Holding holding = new Holding();
        holding.setName(request.name());
        holding.setOwner(person);
        holding.setAccount(account);
        holding.setInstrumentType(instrumentType);
        holding.setUnits(request.units());
        holding.setAcquisitionCostTotal(request.acquisitionCostTotal());
        holding.setAcquisitionCostPerUnit(request.acquisitionCostPerUnit());
        holding.setAcquisitionDate(request.acquisitionDate());
        holding.setCurrentValue(request.currentValue());
        holding.setValuationDate(request.valuationDate());
        holding.setCurrency(request.currency() != null ? request.currency() : "INR");
        holding.setStatus(HoldingStatus.ACTIVE);
        holding.setProvenanceSource("MANUAL");

        return toResponse(holdingRepository.save(holding));
    }

    @Transactional(readOnly = true)
    public HoldingResponse findById(UUID id) {
        return toResponse(holdingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holding not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> findByPersonId(UUID personId) {
        return holdingRepository.findByOwnerId(personId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> findByAccountId(UUID accountId) {
        return holdingRepository.findByAccountId(accountId).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Find all holdings belonging to any member of a household.
     * Used by the dashboard and portfolio page which operate at household level.
     */
    @Transactional(readOnly = true)
    public List<HoldingResponse> findByHouseholdId(UUID householdId) {
        return holdingRepository.findByOwnerHouseholdId(householdId).stream()
                .map(this::toResponse).toList();
    }

    public HoldingResponse update(UUID id, HoldingRequest request) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holding not found: " + id));
        holding.setName(request.name());
        holding.setUnits(request.units());
        holding.setCurrentValue(request.currentValue());
        holding.setValuationDate(request.valuationDate());
        if (request.acquisitionCostTotal() != null) holding.setAcquisitionCostTotal(request.acquisitionCostTotal());
        if (request.acquisitionCostPerUnit() != null) holding.setAcquisitionCostPerUnit(request.acquisitionCostPerUnit());
        return toResponse(holdingRepository.save(holding));
    }

    public void delete(UUID id) {
        if (!holdingRepository.existsById(id)) throw new RuntimeException("Holding not found: " + id);
        holdingRepository.deleteById(id);
    }

    private HoldingResponse toResponse(Holding h) {
        return new HoldingResponse(h.getId(), h.getName(), h.getAccount().getId(),
                h.getOwner().getId(), h.getInstrumentType().getCode(),
                h.getInstrumentType().getDisplayName(), h.getInstrumentType().getAssetClass(),
                h.getUnits(), h.getAcquisitionCostTotal(), h.getAcquisitionCostPerUnit(),
                h.getAcquisitionDate(), h.getCurrentValue(), h.getValuationDate(),
                h.getCurrency(), h.getStatus().name(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
