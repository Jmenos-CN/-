package com.jmens.advisor.common.cache;

import java.time.Duration;
import java.util.Optional;

public class NoopCacheClient implements CacheClient {

  @Override
  public Optional<String> get(String key) {
    return Optional.empty();
  }

  @Override
  public void put(String key, String value, Duration ttl) {
  }
}
