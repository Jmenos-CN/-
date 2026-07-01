package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockQuote;
import org.junit.jupiter.api.Test;

class SinaStockDataClientTest {

  @Test
  void parsesSinaQuotePayload() {
    String raw = "var hq_str_sh600519=\"贵州茅台,1500.00,1490.00,1510.00,1520.00,1488.00,0,0,123456,185000000.00,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2026-07-01,10:30:00\";";

    StockQuote quote = SinaStockDataClient.parseQuote("600519", raw);

    assertThat(quote.code()).isEqualTo("600519");
    assertThat(quote.name()).isEqualTo("贵州茅台");
    assertThat(quote.latestPrice()).isEqualByComparingTo("1510.00");
    assertThat(quote.changePercent()).isEqualByComparingTo("1.34");
    assertThat(quote.volume()).isEqualTo(123456L);
  }
}
