package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BasicValuationServiceTest {

  private final BasicValuationService service = new BasicValuationService();

  @Test
  void explainsPePbRoeAndDebtRatioFromQuoteAndFinancialSnapshot() {
    BasicValuationService.BasicValuation valuation = service.evaluate(quote(), financial());

    assertThat(valuation.pe()).isEqualByComparingTo("69.39");
    assertThat(valuation.pb()).isEqualByComparingTo("6.98");
    assertThat(valuation.text())
        .contains("Basic valuation")
        .contains("PE=69.39")
        .contains("PB=6.98")
        .contains("ROE=10.57%")
        .contains("debtRatio=12.12%")
        .contains("static snapshot")
        .contains("not investment advice");
    assertThat(valuation.evidenceValue()).contains("PE=69.39", "PB=6.98", "ROE=10.57%");
  }

  @Test
  void handlesMissingPerShareMetricsWithoutDivisionByZero() {
    StockFinancialSnapshot financial = new StockFinancialSnapshot(
        "600519",
        "Kweichow Moutai",
        LocalDate.of(2026, 3, 31),
        "Q1",
        BigDecimal.ZERO,
        null,
        new BigDecimal("54702912385.23"),
        new BigDecimal("27242512886.45"),
        new BigDecimal("10.57"),
        new BigDecimal("12.1227489682")
    );

    BasicValuationService.BasicValuation valuation = service.evaluate(quote(), financial);

    assertThat(valuation.pe()).isNull();
    assertThat(valuation.pb()).isNull();
    assertThat(valuation.text())
        .contains("PE=n/a")
        .contains("PB=n/a")
        .contains("not enough per-share metrics");
  }

  @Test
  void returnsUnavailableExplanationWhenFinancialSnapshotIsMissing() {
    BasicValuationService.BasicValuation valuation = service.evaluate(quote(), null);

    assertThat(valuation.available()).isFalse();
    assertThat(valuation.text()).contains("Basic valuation: not available");
    assertThat(valuation.evidenceValue()).contains("not available");
  }

  private StockQuote quote() {
    return new StockQuote(
        "600519",
        "Kweichow Moutai",
        new BigDecimal("1510.00"),
        new BigDecimal("1490.00"),
        new BigDecimal("1.34"),
        123456L,
        new BigDecimal("185000000.00"),
        LocalDateTime.of(2026, 7, 1, 10, 30)
    );
  }

  private StockFinancialSnapshot financial() {
    return new StockFinancialSnapshot(
        "600519",
        "Kweichow Moutai",
        LocalDate.of(2026, 3, 31),
        "Q1",
        new BigDecimal("21.76"),
        new BigDecimal("216.32234994607"),
        new BigDecimal("54702912385.23"),
        new BigDecimal("27242512886.45"),
        new BigDecimal("10.57"),
        new BigDecimal("12.1227489682")
    );
  }
}
