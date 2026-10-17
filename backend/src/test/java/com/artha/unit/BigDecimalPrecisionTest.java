package com.artha.unit;

import com.artha.config.ArthaProperties;
import com.artha.domain.Frequency;
import com.artha.repository.ExpenseRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.IncomeRepository;
import com.artha.repository.LiabilityRepository;
import com.artha.service.CashFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UT-19: BigDecimal precision tests.
 *
 * SPEC REF: NFR-1.2, Test Plan UT-19
 * INPUT: Currency operations at edge (₹0.01 differences)
 * EXPECTED: No floating-point drift
 *
 * WHY THIS TEST EXISTS:
 *   Floating-point (double) arithmetic can produce surprising results:
 *     double a = 0.1 + 0.2;  // Result: 0.30000000000000004, NOT 0.3
 *   This is because 0.1 and 0.2 cannot be exactly represented in binary.
 *
 *   For financial calculations, this is unacceptable. ₹0.01 differences
 *   accumulate over thousands of transactions and produce wrong totals.
 *
 *   BigDecimal stores numbers as exact decimal values (base 10), avoiding
 *   this problem entirely. These tests verify that our calculations
 *   produce exact results with no drift.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-19: BigDecimal Precision")
class BigDecimalPrecisionTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private LiabilityRepository liabilityRepository;

    private CashFlowService cashFlowService;

    @BeforeEach
    void setUp() {
        ArthaProperties props = new ArthaProperties();
        props.setCurrencyScale(2);
        props.setRateScale(6);
        cashFlowService = new CashFlowService(incomeRepository, expenseRepository,
                householdRepository, liabilityRepository, props);
    }

    @Test
    @DisplayName("0.1 + 0.2 = 0.30 exactly (the classic float failure case)")
    void testClassicFloatFailure() {
        // This would FAIL with double: 0.1 + 0.2 = 0.30000000000000004
        BigDecimal a = new BigDecimal("0.10");
        BigDecimal b = new BigDecimal("0.20");
        BigDecimal result = a.add(b).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("0.30"), result);
    }

    @Test
    @DisplayName("Normalizing ₹0.01 annual to monthly preserves precision")
    void testSmallAmountNormalization() {
        // ₹0.01/year → ₹0.00/month (0.01 / 12 = 0.000833... → rounds to 0.00)
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("0.01"), Frequency.ANNUAL);
        assertEquals(new BigDecimal("0.00"), result);
    }

    @Test
    @DisplayName("Large amount normalization — no precision loss")
    void testLargeAmountNormalization() {
        // ₹99,99,999.99/year → ₹8,33,333.33/month
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("9999999.99"), Frequency.ANNUAL);
        assertEquals(new BigDecimal("833333.33"), result);
    }

    @Test
    @DisplayName("Quarterly normalization — exact division")
    void testQuarterlyNormalization() {
        // ₹30,000/quarter → ₹10,000.00/month
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("30000.00"), Frequency.QUARTERLY);
        assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test
    @DisplayName("Quarterly normalization — non-exact division rounds correctly")
    void testQuarterlyNonExact() {
        // ₹10,000/quarter → ₹3,333.33/month (10000/3 = 3333.333... → HALF_UP → 3333.33)
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("10000.00"), Frequency.QUARTERLY);
        assertEquals(new BigDecimal("3333.33"), result);
    }

    @Test
    @DisplayName("Semi-annual normalization")
    void testSemiAnnualNormalization() {
        // ₹60,000/semi-annual → ₹10,000.00/month
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("60000.00"), Frequency.SEMI_ANNUAL);
        assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test
    @DisplayName("One-time amount spread over 12 months")
    void testOneTimeNormalization() {
        // ₹1,20,000 one-time → ₹10,000.00/month
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("120000.00"), Frequency.ONE_TIME);
        assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test
    @DisplayName("Currency scale (2) is enforced")
    void testCurrencyScaleEnforcement() {
        // Monthly amount should always have exactly 2 decimal places
        BigDecimal result = cashFlowService.normalizeToMonthly(
                new BigDecimal("100"), Frequency.MONTHLY);
        assertEquals(2, result.scale());
        assertEquals(new BigDecimal("100.00"), result);
    }
}
