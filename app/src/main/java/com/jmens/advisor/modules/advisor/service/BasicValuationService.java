package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class BasicValuationService {

  private static final int SCALE = 2;

  public BasicValuation evaluate(StockQuote quote, StockFinancialSnapshot financial) {
    if (financial == null) {
      return new BasicValuation(
          false,
          null,
          null,
          "Basic valuation: not available because financial indicators are unavailable.",
          "Basic valuation not available"
      );
    }
    BigDecimal pe = ratio(quote.latestPrice(), financial.eps());
    BigDecimal pb = ratio(quote.latestPrice(), financial.bps());
    String text = buildText(financial, pe, pb);
    String evidence = "PE=%s, PB=%s, ROE=%s%%, debtRatio=%s%%, reportDate=%s"
        .formatted(
            display(pe),
            display(pb),
            display(financial.roe()),
            display(financial.debtRatio()),
            financial.reportDate()
        );
    return new BasicValuation(pe != null || pb != null, pe, pb, text, evidence);
  }

  private String buildText(StockFinancialSnapshot financial, BigDecimal pe, BigDecimal pb) {
    String metricAvailability = pe == null || pb == null
        ? " Some per-share metrics are missing or zero, so not enough per-share metrics are available for a complete PE/PB view."
        : "";
    return "Basic valuation: PE=%s, PB=%s, ROE=%s%%, debtRatio=%s%% based on reportDate=%s. "
        .formatted(
            display(pe),
            display(pb),
            display(financial.roe()),
            display(financial.debtRatio()),
            financial.reportDate()
        )
        + "This is a static snapshot based on latest price and reported per-share metrics; it is not a forecast, target price, and not investment advice."
        + metricAvailability;
  }

  private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
  }

  private String display(BigDecimal value) {
    if (value == null) {
      return "n/a";
    }
    return value.setScale(SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }

  public record BasicValuation(
      boolean available,
      BigDecimal pe,
      BigDecimal pb,
      String text,
      String evidenceValue
  ) {}
}
