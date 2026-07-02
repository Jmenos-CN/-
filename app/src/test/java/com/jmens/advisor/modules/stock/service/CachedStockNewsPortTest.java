package com.jmens.advisor.modules.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CachedStockNewsPortTest {

  @Test
  void cachesRecentNewsByStockCodeAndLimit() {
    CountingStockNewsPort delegate = new CountingStockNewsPort();
    CachedStockNewsPort cachedPort = new CachedStockNewsPort(
        delegate,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, null, Duration.ofMinutes(30), null)
    );

    List<StockNewsItem> first = cachedPort.getRecentNews(new StockSymbol("600519", "SH"), 3);
    List<StockNewsItem> second = cachedPort.getRecentNews(new StockSymbol("600519", "SH"), 3);

    assertThat(first).hasSize(1);
    assertThat(second).hasSize(1);
    assertThat(second.get(0).title()).isEqualTo("贵州茅台新闻");
    assertThat(delegate.calls).isEqualTo(1);
  }

  private static class CountingStockNewsPort implements StockNewsPort {

    private int calls;

    @Override
    public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
      calls++;
      return List.of(new StockNewsItem(
          "贵州茅台新闻",
          "https://finance.sina.com.cn/news1.shtml",
          LocalDateTime.of(2026, 7, 2, 17, 20),
          "Sina Finance"
      ));
    }
  }
}
