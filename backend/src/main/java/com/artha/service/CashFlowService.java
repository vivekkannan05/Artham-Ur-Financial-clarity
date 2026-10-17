package com.artha.service;

import com.artha.config.ArthaProperties;
import com.artha.domain.Expense;
import com.artha.domain.ExpenseCategory;
import com.artha.domain.Frequency;
import com.artha.domain.Income;
import com.artha.dto.CashFlowStatement;
import com.artha.repository.ExpenseRepository;
import com.artha.repository.HouseholdRepository;
import com.artha.repository.IncomeRepository;
import com.artha.repository.LiabilityRepository;
import com.artha.domain.Liability;
import com.artha.domain.LiabilityStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Cash Flow Engine — Computes monthly cash-flow statement for a household.
 *
 * SPEC REF: FR-2.3 — "Monthly cash-flow statement per household: gross income,
 * essential expenses, discretionary spending, EMI payments, investment outflows (SIPs),
 * free cash flow, savings rate (savings/gross income),
 * debt-service ratio (EMIs/gross income)."
 *
 * SPEC REF: NFR-1.2 — All calculations use BigDecimal with ROUND_HALF_UP.
 *   Currency: 2-decimal precision.  Rates: 6-decimal precision.
 *
 * ALGORITHM (from Design Doc Section 8):
 *   1. Load all incomes for the household → normalize each to monthly → sum
 *   2. Load all expenses for the household → normalize each to monthly → split by category
 *   3. Compute:
 *      - grossIncome = sum of monthly incomes
 *      - consumptionExpenses = sum of CONSUMPTION expenses (monthly)
 *      - investmentOutflows = sum of INVESTMENT_OUTFLOW expenses (monthly)
 *      - totalExpenses = consumptionExpenses + investmentOutflows
 *      - freeCashFlow = grossIncome - totalExpenses
 *      - savingsRate = (freeCashFlow / grossIncome) * 100
 *      - debtServiceRatio = totalActiveEMIs / grossIncome * 100
 *
 * WHY NORMALIZE TO MONTHLY?
 *   Incomes and expenses can have different frequencies (annual salary,
 *   monthly rent, quarterly insurance). To compare them, we convert
 *   everything to a monthly equivalent:
 *     Annual  → divide by 12
 *     Monthly → as-is
 *     Quarterly → divide by 3
 *     Semi-annual → divide by 6
 *     One-time → divide by 12 (spread over a year)
 */
@Service
@Transactional(readOnly = true)
public class CashFlowService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final HouseholdRepository householdRepository;
    private final LiabilityRepository liabilityRepository;
    private final ArthaProperties arthaProperties;

    public CashFlowService(IncomeRepository incomeRepository,
                           ExpenseRepository expenseRepository,
                           HouseholdRepository householdRepository,
                           LiabilityRepository liabilityRepository,
                           ArthaProperties arthaProperties) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.householdRepository = householdRepository;
        this.liabilityRepository = liabilityRepository;
        this.arthaProperties = arthaProperties;
    }

    /**
     * Compute the monthly cash-flow statement for a household.
     *
     * This is the primary cash-flow calculation per FR-2.3.
     * All amounts in the returned statement are monthly equivalents.
     */
    public CashFlowStatement computeMonthlyCashFlow(UUID householdId) {
        // Verify household exists
        if (!householdRepository.existsById(householdId)) {
            throw new RuntimeException("Household not found: " + householdId);
        }

        int currencyScale = arthaProperties.getCurrencyScale();
        int rateScale = arthaProperties.getRateScale();

        // ─── Step 1: Sum all incomes (normalized to monthly) ───
        List<Income> incomes = incomeRepository.findByPersonHouseholdId(householdId);
        BigDecimal grossIncome = incomes.stream()
                .map(income -> normalizeToMonthly(income.getAmount(), income.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(currencyScale, RoundingMode.HALF_UP);

        // ─── Step 2: Sum expenses by category (normalized to monthly) ───
        List<Expense> expenses = expenseRepository.findByPersonHouseholdId(householdId);

        BigDecimal consumptionExpenses = expenses.stream()
                .filter(e -> e.getCategory() == ExpenseCategory.CONSUMPTION)
                .map(e -> normalizeToMonthly(e.getAmount(), e.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(currencyScale, RoundingMode.HALF_UP);

        BigDecimal investmentOutflows = expenses.stream()
                .filter(e -> e.getCategory() == ExpenseCategory.INVESTMENT_OUTFLOW)
                .map(e -> normalizeToMonthly(e.getAmount(), e.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(currencyScale, RoundingMode.HALF_UP);

        // ─── Step 3: Compute derived values ───
        BigDecimal totalExpenses = consumptionExpenses.add(investmentOutflows)
                .setScale(currencyScale, RoundingMode.HALF_UP);

        BigDecimal freeCashFlow = grossIncome.subtract(totalExpenses)
                .setScale(currencyScale, RoundingMode.HALF_UP);

        // Savings rate = (free cash flow / gross income) * 100
        // Handle division by zero: if no income, savings rate is 0
        BigDecimal savingsRate;
        if (grossIncome.compareTo(BigDecimal.ZERO) == 0) {
            savingsRate = BigDecimal.ZERO.setScale(rateScale, RoundingMode.HALF_UP);
        } else {
            savingsRate = freeCashFlow
                    .multiply(BigDecimal.valueOf(100))
                    .divide(grossIncome, rateScale, RoundingMode.HALF_UP);
        }

        // Debt-service ratio: total active EMIs / gross income (FR-2.3, updated in Batch 4).
        // This ratio tells how much of the household income goes to debt repayment.
        // Healthy: < 20%.  Warning: 20-40%.  Critical: > 40%.
        BigDecimal totalEmi = liabilityRepository
                .findByHouseholdIdAndStatus(householdId, LiabilityStatus.ACTIVE)
                .stream()
                .map(Liability::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debtServiceRatio;
        if (grossIncome.compareTo(BigDecimal.ZERO) == 0) {
            debtServiceRatio = BigDecimal.ZERO.setScale(rateScale, RoundingMode.HALF_UP);
        } else {
            debtServiceRatio = totalEmi
                    .multiply(BigDecimal.valueOf(100))
                    .divide(grossIncome, rateScale, RoundingMode.HALF_UP);
        }

        return new CashFlowStatement(
                householdId,
                grossIncome,
                consumptionExpenses,
                investmentOutflows,
                totalExpenses,
                freeCashFlow,
                savingsRate,
                debtServiceRatio
        );
    }

    /**
     * Normalize an amount to its monthly equivalent based on frequency.
     *
     * Examples:
     *   ₹12,00,000/year → ₹1,00,000/month
     *   ₹30,000/quarter → ₹10,000/month
     *   ₹5,000/month    → ₹5,000/month (no change)
     *   ₹60,000 one-time → ₹5,000/month (spread over 12 months)
     *
     * This method is package-private so it can be tested directly.
     */
    public BigDecimal normalizeToMonthly(BigDecimal amount, Frequency frequency) {
        int scale = arthaProperties.getCurrencyScale();
        return switch (frequency) {
            case MONTHLY -> amount.setScale(scale, RoundingMode.HALF_UP);
            case ANNUAL -> amount.divide(BigDecimal.valueOf(12), scale, RoundingMode.HALF_UP);
            case QUARTERLY -> amount.divide(BigDecimal.valueOf(3), scale, RoundingMode.HALF_UP);
            case SEMI_ANNUAL -> amount.divide(BigDecimal.valueOf(6), scale, RoundingMode.HALF_UP);
            case ONE_TIME -> amount.divide(BigDecimal.valueOf(12), scale, RoundingMode.HALF_UP);
        };
    }
}
