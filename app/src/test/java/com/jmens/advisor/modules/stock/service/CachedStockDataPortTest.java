package com.jmens.advisor.modules.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CachedStockDataPortTest {

  @Test
  void cachesRealtimeQuoteByStockCode() {
    CountingStockDataPort delegate = new CountingStockDataPort();
    CachedStockDataPort cachedPort = new CachedStockDataPort(
        delegate,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(Duration.ofSeconds(15), null, null, null, null)
    );

    StockQuote first = cachedPort.getRealtimeQuote(new StockSymbol("600519", "SH"));
    StockQuote second = cachedPort.getRealtimeQuote(new StockSymbol("600519", "SH"));

    assertThat(first.latestPrice()).isEqualByComparingTo("1200.00");
    assertThat(second.latestPrice()).isEqualByComparingTo("1200.00");
    assertThat(delegate.quoteCalls).isEqualTo(1);
  }

  private static class CountingStockDataPort implements StockDataPort {

    private int quoteCalls;

    @Override
    public StockQuote getRealtimeQuote(StockSymbol symbol) {
      quoteCalls++;
      return new StockQuote(
          symbol.code(),
          "贵州茅台",
          new BigDecimal("1200.00"),
          new BigDecimal("1190.00"),
          new BigDecimal("0.84"),
          1000L,
          new BigDecimal("1200000.00"),
          LocalDateTime.of(2026, 7, 2, 10, 0)
      );
    }

    @Override
    public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
      return List.of();
    }
  }
}
