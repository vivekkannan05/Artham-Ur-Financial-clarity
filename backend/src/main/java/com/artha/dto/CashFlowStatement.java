package com.artha.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cash Flow Statement for a household (monthly view).
 *
 * SPEC REF: FR-2.3 — "Monthly cash-flow statement per household: gross income,
 * essential expenses, discretionary spending, EMI payments, investment outflows,
 * free cash flow, savings rate, debt-service ratio."
 *
 * All amounts use BigDecimal (per NFR-1.2).
 *
 * FORMULA BREAKDOWN:
 *   grossIncome         = sum of all incomes (normalized to monthly)
 *   consumptionExpenses = sum of CONSUMPTION expenses (normalized to monthly)
 *   investmentOutflows  = sum of INVESTMENT_OUTFLOW expenses (SIPs, EMIs)
 *   totalExpenses       = consumptionExpenses + investmentOutflows
 *   freeCashFlow        = grossIncome - totalExpenses
 *   savingsRate         = freeCashFlow / grossIncome  (as percentage)
 *   debtServiceRatio    = emiPayments / grossIncome   (as percentage)
 *
 * NOTE: In Batch 1 we don't have EMI data (liabilities come in Batch 4),
 * so emiPayments will be 0 and debtServiceRatio will be 0.
 */
public record CashFlowStatement(
        UUID householdId,
        BigDecimal grossIncome,
        BigDecimal consumptionExpenses,
        BigDecimal investmentOutflows,
        BigDecimal totalExpenses,
        BigDecimal freeCashFlow,
        BigDecimal savingsRate,             // as percentage (e.g., 25.50 means 25.50%)
        BigDecimal debtServiceRatio         // as percentage (0 until Batch 4 adds liabilities)
) {}
