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
import java.time.LocalDate;
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

  @Test
  void cachesRecentKLineByStockCodeAndDays() {
    CountingStockDataPort delegate = new CountingStockDataPort();
    CachedStockDataPort cachedPort = new CachedStockDataPort(
        delegate,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, Duration.ofHours(1), null, null, null)
    );

    List<KLinePoint> first = cachedPort.getRecentKLine(new StockSymbol("600519", "SH"), 5);
    List<KLinePoint> second = cachedPort.getRecentKLine(new StockSymbol("600519", "SH"), 5);

    assertThat(first).hasSize(1);
    assertThat(second).hasSize(1);
    assertThat(second.get(0).close()).isEqualByComparingTo("1201.00");
    assertThat(delegate.klineCalls).isEqualTo(1);
  }

  private static class CountingStockDataPort implements StockDataPort {

    private int quoteCalls;
    private int klineCalls;

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
      klineCalls++;
      return List.of(new KLinePoint(
          LocalDate.of(2026, 7, 2),
          new BigDecimal("1190.00"),
          new BigDecimal("1201.00"),
          new BigDecimal("1210.00"),
          new BigDecimal("1188.00"),
          10000L
      ));
    }
  }
}
