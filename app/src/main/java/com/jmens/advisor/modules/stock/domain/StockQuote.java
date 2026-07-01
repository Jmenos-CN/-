package com.jmens.advisor.modules.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockQuote(
    String code,
    String name,
    BigDecimal latestPrice,
    BigDecimal previousClose,
    BigDecimal changePercent,
    long volume,
    BigDecimal amount,
    LocalDateTime quoteTime
) {}
