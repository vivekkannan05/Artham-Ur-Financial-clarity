package com.artha.unit;

import com.artha.domain.Frequency;
import com.artha.domain.Income;
import com.artha.domain.IncomeType;
import com.artha.domain.Person;
import com.artha.domain.Transaction;
import com.artha.domain.TransactionType;
import com.artha.dto.TaxFlag;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.TransactionRepository;
import com.artha.service.TaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * UT-15: Tax clubbing flag on spouse transfer.
 *
 * SPEC REF: FR-6.1, EC-3 — "When spouse transfers asset to other spouse,
 * flag clubbing tax implication."
 *
 * TEST SCENARIO:
 *   Person A (husband) transfers ₹10,00,000 to Person B (wife)'s FD account.
 *   The tax service should raise a CLUBBING flag with Section 64 reference.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-15: Tax Clubbing Detection")
class TaxServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private HouseholdRepository householdRepository;

    private TaxService taxService;
    private final UUID householdId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taxService = new TaxService(transactionRepository, householdRepository);
    }

    @Test
    @DisplayName("UT-15: Transfer from spouse A to spouse B → clubbing flag raised")
    void testClubbingFlagOnSpouseTransfer() {
        when(householdRepository.existsById(householdId)).thenReturn(true);

        Person husband = createPerson("Rahul");
        Transaction transfer = createTransfer(husband, new BigDecimal("1000000"), "Gift from Rahul");

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of(transfer));

        List<TaxFlag> flags = taxService.scanForFlags(householdId);

        assertEquals(1, flags.size());
        TaxFlag flag = flags.get(0);
        assertEquals("CLUBBING", flag.flagType());
        assertEquals("WARNING", flag.severity());
        assertTrue(flag.message().contains("Section 64"));
        assertTrue(flag.message().contains("Rahul"));
        assertEquals(new BigDecimal("1000000"), flag.amount());
    }

    @Test
    @DisplayName("No transfers → no clubbing flags")
    void testNoTransfersNoFlags() {
        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of());

        List<TaxFlag> flags = taxService.scanForFlags(householdId);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("Non-transfer transaction type → no clubbing flag")
    void testBuyTransactionNoFlag() {
        when(householdRepository.existsById(householdId)).thenReturn(true);

        Person person = createPerson("Rahul");
        Transaction buy = createBuyTransaction(person, new BigDecimal("50000"));

        when(transactionRepository.findByHoldingAccountOwnerHouseholdId(householdId))
                .thenReturn(List.of(buy));

        List<TaxFlag> flags = taxService.scanForFlags(householdId);
        assertTrue(flags.isEmpty());
    }

    @Test
    @DisplayName("EPF tax threshold constant matches spec binding")
    void testEpfThresholdMatchesSpec() {
        assertEquals(new BigDecimal("250000"), TaxService.EPF_TAX_THRESHOLD,
                "EPF_TAX_THRESHOLD must be 250000 per SPEC-BINDING FR-6.1");
    }

    // ─── Test helpers ────────────────────────────────────────

    private Person createPerson(String name) {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setName(name);
        return p;
    }

    private Transaction createTransfer(Person owner, BigDecimal amount, String sourceOfFunds) {
        Transaction t = new Transaction();
        t.setOwner(owner);
        t.setTransactionType(TransactionType.TRANSFER);
        t.setAmount(amount);
        t.setTransactionDate(LocalDate.now());
        t.setSourceOfFunds(sourceOfFunds);
        return t;
    }

    private Transaction createBuyTransaction(Person owner, BigDecimal amount) {
        Transaction t = new Transaction();
        t.setOwner(owner);
        t.setTransactionType(TransactionType.BUY);
        t.setAmount(amount);
        t.setTransactionDate(LocalDate.now());
        return t;
    }
}
