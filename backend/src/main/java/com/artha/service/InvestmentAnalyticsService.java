package com.artha.service;

import com.artha.domain.Holding;
import com.artha.domain.Transaction;
import com.artha.domain.TransactionType;
import com.artha.dto.InvestmentAnalytics;
import com.artha.engine.CagrCalculator;
import com.artha.engine.CashFlow;
import com.artha.engine.XirrCalculator;
import com.artha.repository.HoldingRepository;
import com.artha.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Investment Analytics Service — Computes XIRR, CAGR, returns for holdings.
 *
 * SPEC REF: FR-3.6 — "XIRR, CAGR, absolute returns, rolling returns"
 *
 * This service builds CashFlow lists from stored transactions, then delegates
 * to the XirrCalculator and CagrCalculator engines.
 */
@Service
@Transactional(readOnly = true)
public class InvestmentAnalyticsService {

    /** Transaction types that represent money going OUT (investments) */
    private static final Set<TransactionType> OUTFLOW_TYPES = Set.of(
            TransactionType.BUY, TransactionType.SIP
    );

    /** Transaction types that represent money coming IN (returns) */
    private static final Set<TransactionType> INFLOW_TYPES = Set.of(
            TransactionType.SELL, TransactionType.DIVIDEND, TransactionType.INTEREST,
            TransactionType.REDEMPTION
    );

    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;

    public InvestmentAnalyticsService(TransactionRepository transactionRepository,
                                      HoldingRepository holdingRepository) {
        this.transactionRepository = transactionRepository;
        this.holdingRepository = holdingRepository;
    }

    /**
     * Compute investment analytics for a holding.
     *
     * XIRR uses all transactions + the current value as a final "inflow."
     * CAGR uses total invested vs current value over the holding period.
     */
    public InvestmentAnalytics computeForHolding(UUID holdingId) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new RuntimeException("Holding not found: " + holdingId));

        List<Transaction> transactions = transactionRepository
                .findByHoldingIdOrderByTransactionDateAsc(holdingId);

        // Build cash flow list for XIRR
        List<CashFlow> cashFlows = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (OUTFLOW_TYPES.contains(txn.getTransactionType())) {
                cashFlows.add(CashFlow.investment(txn.getTransactionDate(), txn.getAmount()));
                totalInvested = totalInvested.add(txn.getAmount());
            } else if (INFLOW_TYPES.contains(txn.getTransactionType())) {
                cashFlows.add(CashFlow.redemption(txn.getTransactionDate(), txn.getAmount()));
            }
        }

        // Add current value as a final positive cash flow (if holding is active)
        BigDecimal currentValue = holding.getCurrentValue() != null
                ? holding.getCurrentValue() : BigDecimal.ZERO;
        LocalDate valuationDate = holding.getValuationDate() != null
                ? holding.getValuationDate() : LocalDate.now();

        if (currentValue.signum() > 0) {
            cashFlows.add(CashFlow.redemption(valuationDate, currentValue));
        }

        // Compute XIRR
        BigDecimal xirr = XirrCalculator.calculate(cashFlows).orElse(null);

        // Compute CAGR
        BigDecimal cagr = null;
        if (totalInvested.signum() > 0 && currentValue.signum() > 0 && !transactions.isEmpty()) {
            LocalDate firstDate = transactions.getFirst().getTransactionDate();
            long daysBetween = ChronoUnit.DAYS.between(firstDate, valuationDate);
            if (daysBetween > 0) {
                BigDecimal years = BigDecimal.valueOf(daysBetween)
                        .divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
                cagr = CagrCalculator.calculate(totalInvested, currentValue, years).orElse(null);
            }
        }

        // Absolute return
        BigDecimal absoluteReturn = currentValue.subtract(totalInvested)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal absoluteReturnPct = totalInvested.signum() > 0
                ? absoluteReturn.multiply(BigDecimal.valueOf(100))
                        .divide(totalInvested, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new InvestmentAnalytics(xirr, cagr, absoluteReturn, absoluteReturnPct);
    }
}
