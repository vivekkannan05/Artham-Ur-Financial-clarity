package com.artha.unit;

import com.artha.config.ArthaProperties;
import com.artha.domain.Expense;
import com.artha.domain.ExpenseCategory;
import com.artha.domain.Frequency;
import com.artha.domain.Income;
import com.artha.domain.IncomeType;
import com.artha.dto.CashFlowStatement;
import com.artha.repository.ExpenseRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.IncomeRepository;
import com.artha.service.CashFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * UT-07: Cash-flow engine tests.
 *
 * SPEC REF: FR-2.3, Test Plan UT-07
 * INPUT: Income entries + expense entries + EMIs + SIPs
 * EXPECTED: Correct free cash flow, savings rate, debt-service ratio
 *
 * TESTING APPROACH:
 *   We use Mockito to mock the repositories — no database needed.
 *   This keeps tests fast (<1 second) and isolated from DB state.
 *
 * WHY @ExtendWith(MockitoExtension.class)?
 *   Tells JUnit 5 to initialize @Mock fields before each test.
 *   Without it, mocks would be null.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-07: Cash Flow Engine")
class CashFlowServiceTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private HouseholdRepository householdRepository;

    private CashFlowService cashFlowService;
    private final UUID householdId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ArthaProperties props = new ArthaProperties();
        props.setCurrencyScale(2);
        props.setRateScale(6);
        cashFlowService = new CashFlowService(incomeRepository, expenseRepository,
                householdRepository, props);
    }

    @Test
    @DisplayName("Monthly salary + consumption + SIP → correct cash flow")
    void testBasicMonthlyCashFlow() {
        // GIVEN: ₹1,00,000 monthly salary
        Income salary = createIncome(IncomeType.SALARY, "100000", Frequency.MONTHLY);

        // GIVEN: ₹30,000 monthly consumption (rent, groceries, etc.)
        Expense rent = createExpense("Rent", "20000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);
        Expense groceries = createExpense("Groceries", "10000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);

        // GIVEN: ₹15,000 monthly SIP (investment outflow)
        Expense sip = createExpense("SIP - Axis Bluechip", "15000", Frequency.MONTHLY, ExpenseCategory.INVESTMENT_OUTFLOW);

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(salary));
        when(expenseRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(rent, groceries, sip));

        // WHEN
        CashFlowStatement result = cashFlowService.computeMonthlyCashFlow(householdId);

        // THEN
        assertEquals(new BigDecimal("100000.00"), result.grossIncome());
        assertEquals(new BigDecimal("30000.00"), result.consumptionExpenses());
        assertEquals(new BigDecimal("15000.00"), result.investmentOutflows());
        assertEquals(new BigDecimal("45000.00"), result.totalExpenses());
        assertEquals(new BigDecimal("55000.00"), result.freeCashFlow());
        // Savings rate: 55000 / 100000 * 100 = 55.000000%
        assertEquals(new BigDecimal("55.000000"), result.savingsRate());
    }

    @Test
    @DisplayName("Annual income normalized to monthly")
    void testAnnualIncomeNormalization() {
        // GIVEN: ₹12,00,000/year salary → ₹1,00,000/month
        Income annualSalary = createIncome(IncomeType.SALARY, "1200000", Frequency.ANNUAL);

        // GIVEN: ₹50,000 monthly consumption
        Expense consumption = createExpense("Living", "50000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(annualSalary));
        when(expenseRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(consumption));

        CashFlowStatement result = cashFlowService.computeMonthlyCashFlow(householdId);

        assertEquals(new BigDecimal("100000.00"), result.grossIncome());
        assertEquals(new BigDecimal("50000.00"), result.consumptionExpenses());
        assertEquals(new BigDecimal("50000.00"), result.freeCashFlow());
    }

    @Test
    @DisplayName("Mixed frequencies: monthly salary + quarterly bonus + annual insurance")
    void testMixedFrequencies() {
        // ₹80,000/month salary + ₹60,000/quarter bonus
        Income salary = createIncome(IncomeType.SALARY, "80000", Frequency.MONTHLY);
        Income bonus = createIncome(IncomeType.BONUS, "60000", Frequency.QUARTERLY);
        // Bonus monthly: 60000/3 = 20000

        // ₹36,000/annual insurance premium (investment outflow)
        Expense insurance = createExpense("Insurance", "36000", Frequency.ANNUAL, ExpenseCategory.INVESTMENT_OUTFLOW);
        // Insurance monthly: 36000/12 = 3000

        // ₹40,000/month consumption
        Expense living = createExpense("Living", "40000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(salary, bonus));
        when(expenseRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(insurance, living));

        CashFlowStatement result = cashFlowService.computeMonthlyCashFlow(householdId);

        // Gross: 80000 + 20000 = 100000
        assertEquals(new BigDecimal("100000.00"), result.grossIncome());
        assertEquals(new BigDecimal("40000.00"), result.consumptionExpenses());
        assertEquals(new BigDecimal("3000.00"), result.investmentOutflows());
        assertEquals(new BigDecimal("43000.00"), result.totalExpenses());
        assertEquals(new BigDecimal("57000.00"), result.freeCashFlow());
    }

    @Test
    @DisplayName("Zero income → savings rate is 0 (no division by zero)")
    void testZeroIncome() {
        Expense expense = createExpense("Food", "5000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of());
        when(expenseRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(expense));

        CashFlowStatement result = cashFlowService.computeMonthlyCashFlow(householdId);

        assertEquals(new BigDecimal("0.00"), result.grossIncome());
        assertEquals(new BigDecimal("-5000.00"), result.freeCashFlow());
        assertEquals(new BigDecimal("0.000000"), result.savingsRate());
    }

    @Test
    @DisplayName("Negative cash flow (expenses > income) — per EC-7")
    void testNegativeCashFlow() {
        // SPEC REF: EC-7 — "Negative cash flow: Allow the data entry (it reflects reality)."
        Income salary = createIncome(IncomeType.SALARY, "50000", Frequency.MONTHLY);
        Expense expenses = createExpense("EMI + SIP + Living", "70000", Frequency.MONTHLY, ExpenseCategory.CONSUMPTION);

        when(householdRepository.existsById(householdId)).thenReturn(true);
        when(incomeRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(salary));
        when(expenseRepository.findByPersonHouseholdId(householdId)).thenReturn(List.of(expenses));

        CashFlowStatement result = cashFlowService.computeMonthlyCashFlow(householdId);

        // Free cash flow is negative — this is allowed per EC-7
        assertEquals(new BigDecimal("-20000.00"), result.freeCashFlow());
        // Savings rate is negative
        assertEquals(new BigDecimal("-40.000000"), result.savingsRate());
    }

    // ─── Helper methods ──────────────────────────────────────

    private Income createIncome(IncomeType type, String amount, Frequency frequency) {
        Income income = new Income();
        income.setIncomeType(type);
        income.setAmount(new BigDecimal(amount));
        income.setFrequency(frequency);
        income.setStartDate(LocalDate.of(2024, 1, 1));
        return income;
    }

    private Expense createExpense(String name, String amount, Frequency frequency, ExpenseCategory category) {
        Expense expense = new Expense();
        expense.setName(name);
        expense.setAmount(new BigDecimal(amount));
        expense.setFrequency(frequency);
        expense.setCategory(category);
        expense.setStartDate(LocalDate.of(2024, 1, 1));
        return expense;
    }
}
