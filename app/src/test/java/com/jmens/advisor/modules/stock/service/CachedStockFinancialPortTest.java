package com.jmens.advisor.modules.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CachedStockFinancialPortTest {

  @Test
  void cachesFinancialSnapshotByStockCode() {
    CountingStockFinancialPort delegate = new CountingStockFinancialPort();
    CachedStockFinancialPort cachedPort = new CachedStockFinancialPort(
        delegate,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, Duration.ofDays(1), null, null)
    );

    StockFinancialSnapshot first = cachedPort.getLatestSnapshot(new StockSymbol("600519", "SH"));
    StockFinancialSnapshot second = cachedPort.getLatestSnapshot(new StockSymbol("600519", "SH"));

    assertThat(first.eps()).isEqualByComparingTo("21.76");
    assertThat(second.roe()).isEqualByComparingTo("10.57");
    assertThat(delegate.calls).isEqualTo(1);
  }

  private static class CountingStockFinancialPort implements StockFinancialPort {

    private int calls;

    @Override
    public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
      calls++;
      return new StockFinancialSnapshot(
          symbol.code(),
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
}
