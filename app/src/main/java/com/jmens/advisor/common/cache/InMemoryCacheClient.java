package com.jmens.advisor.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheClient implements CacheClient {

  private final Map<String, Entry> values = new ConcurrentHashMap<>();

  @Override
  public Optional<String> get(String key) {
    Entry entry = values.get(key);
    if (entry == null) {
      return Optional.empty();
    }
    if (entry.expiresAt().isBefore(Instant.now())) {
      values.remove(key);
      return Optional.empty();
    }
    return Optional.of(entry.value());
  }

  @Override
  public void put(String key, String value, Duration ttl) {
    values.put(key, new Entry(value, Instant.now().plus(ttl)));
  }

  private record Entry(String value, Instant expiresAt) {}
}
