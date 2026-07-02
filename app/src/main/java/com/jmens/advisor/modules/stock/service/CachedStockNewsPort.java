package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.common.cache.CacheKey;
import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.infrastructure.SinaStockNewsClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class CachedStockNewsPort implements StockNewsPort {

  private final StockNewsPort delegate;
  private final JsonCacheService cacheService;
  private final CacheTtlProperties ttlProperties;

  @Autowired
  public CachedStockNewsPort(
      SinaStockNewsClient delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this((StockNewsPort) delegate, cacheService, ttlProperties);
  }

  CachedStockNewsPort(
      StockNewsPort delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this.delegate = delegate;
    this.cacheService = cacheService;
    this.ttlProperties = ttlProperties;
  }

  @Override
  public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
    String key = CacheKey.news(symbol.code(), limit);
    return cacheService.getList(key, StockNewsItem.class)
        .orElseGet(() -> {
          List<StockNewsItem> news = delegate.getRecentNews(symbol, limit);
          cacheService.put(key, news, ttlProperties.news());
          return news;
        });
  }
}
