package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.common.cache.CacheKey;
import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.infrastructure.SinaStockDataClient;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Adds a short cache in front of market data because quote reads are external I/O.
 */
@Primary
@Service
public class CachedStockDataPort implements StockDataPort {

  private final StockDataPort delegate;
  private final JsonCacheService cacheService;
  private final CacheTtlProperties ttlProperties;

  @Autowired
  public CachedStockDataPort(
      SinaStockDataClient delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this((StockDataPort) delegate, cacheService, ttlProperties);
  }

  CachedStockDataPort(
      StockDataPort delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this.delegate = delegate;
    this.cacheService = cacheService;
    this.ttlProperties = ttlProperties;
  }

  @Override
  public StockQuote getRealtimeQuote(StockSymbol symbol) {
    String key = CacheKey.quote(symbol.code());
    return cacheService.get(key, StockQuote.class)
        .orElseGet(() -> {
          StockQuote quote = delegate.getRealtimeQuote(symbol);
          cacheService.put(key, quote, ttlProperties.quote());
          return quote;
        });
  }

  @Override
  public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
    String key = CacheKey.kline(symbol.code(), days);
    return cacheService.getList(key, KLinePoint.class)
        .orElseGet(() -> {
          List<KLinePoint> points = delegate.getRecentKLine(symbol, days);
          cacheService.put(key, points, ttlProperties.kline());
          return points;
        });
  }
}
