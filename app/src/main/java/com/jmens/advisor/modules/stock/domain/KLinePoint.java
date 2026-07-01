package com.jmens.advisor.modules.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KLinePoint(
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal close,
    BigDecimal high,
    BigDecimal low,
    long volume
) {}
