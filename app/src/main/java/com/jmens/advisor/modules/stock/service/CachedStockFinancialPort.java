package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.common.cache.CacheKey;
import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.infrastructure.EastmoneyFinancialClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class CachedStockFinancialPort implements StockFinancialPort {

  private final StockFinancialPort delegate;
  private final JsonCacheService cacheService;
  private final CacheTtlProperties ttlProperties;

  @Autowired
  public CachedStockFinancialPort(
      EastmoneyFinancialClient delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this((StockFinancialPort) delegate, cacheService, ttlProperties);
  }

  CachedStockFinancialPort(
      StockFinancialPort delegate,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this.delegate = delegate;
    this.cacheService = cacheService;
    this.ttlProperties = ttlProperties;
  }

  @Override
  public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
    String key = CacheKey.finance(symbol.code());
    return cacheService.get(key, StockFinancialSnapshot.class)
        .orElseGet(() -> {
          StockFinancialSnapshot snapshot = delegate.getLatestSnapshot(symbol);
          cacheService.put(key, snapshot, ttlProperties.finance());
          return snapshot;
        });
  }
}
