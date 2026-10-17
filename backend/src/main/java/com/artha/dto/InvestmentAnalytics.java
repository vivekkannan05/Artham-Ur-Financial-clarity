package com.artha.dto;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Investment analytics for a holding.
 * SPEC REF: FR-3.6
 */
public record InvestmentAnalytics(
        BigDecimal xirr,            // null if insufficient data
        BigDecimal cagr,            // null if insufficient data
        BigDecimal absoluteReturn,
        BigDecimal absoluteReturnPct
) {}
