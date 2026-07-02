package com.jmens.advisor.modules.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockFinancialSnapshot(
    String stockCode,
    String stockName,
    LocalDate reportDate,
    String reportType,
    BigDecimal eps,
    BigDecimal bps,
    BigDecimal totalOperatingRevenue,
    BigDecimal parentNetProfit,
    BigDecimal roe,
    BigDecimal debtRatio
) {}
