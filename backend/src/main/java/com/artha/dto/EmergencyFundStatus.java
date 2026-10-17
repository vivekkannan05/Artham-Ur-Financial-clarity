package com.artha.dto;

import com.artha.domain.EmergencyFundAdequacy;
import java.math.BigDecimal;

/**
 * Emergency Fund Status — How prepared is the household for an emergency?
 *
 * SPEC REF: FR-5.2 — "Calculate emergency fund adequacy: required emergency reserve
 * (configurable multiplier × monthly essential expenses, default: 6 months),
 * current liquid reserve (sum of savings accounts + liquid MF + FD maturing
 * within 3 months), months covered, shortfall, and adequacy status."
 *
 * FORMULA:
 *   requiredReserve = EMERGENCY_FUND_MONTHS × monthlyEssentialExpenses
 *   currentLiquidReserve = Σ(SAVINGS account holdings) + Σ(LIQUID_FUND holdings) + Σ(short-term FD)
 *   monthsCovered = currentLiquidReserve / monthlyEssentialExpenses
 *   shortfall = max(0, requiredReserve - currentLiquidReserve)
 *   adequacy = ADEQUATE if monthsCovered >= target
 *              PARTIAL if monthsCovered >= target / 2
 *              CRITICAL if monthsCovered < target / 2
 *
 * WHY "ESSENTIAL" EXPENSES (not total expenses)?
 *   In an emergency (job loss, health crisis), you'd cut discretionary spending.
 *   Emergency fund needs to cover only essentials: rent, food, utilities, insurance premiums.
 *   That's the CONSUMPTION category, not INVESTMENT_OUTFLOW (SIPs can be paused).
 */
public record EmergencyFundStatus(
        BigDecimal monthlyEssentialExpenses,
        BigDecimal requiredReserve,
        BigDecimal currentLiquidReserve,
        BigDecimal monthsCovered,
        BigDecimal shortfall,
        EmergencyFundAdequacy adequacy
) {}
