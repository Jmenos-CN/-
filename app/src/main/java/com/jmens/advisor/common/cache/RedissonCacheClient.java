package com.jmens.advisor.common.cache;

import java.time.Duration;
import java.util.Optional;
import org.redisson.api.RedissonClient;

public class RedissonCacheClient implements CacheClient {

  private final RedissonClient redissonClient;

  public RedissonCacheClient(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public Optional<String> get(String key) {
    try {
      return Optional.ofNullable(redissonClient.<String>getBucket(key).get());
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  @Override
  public void put(String key, String value, Duration ttl) {
    try {
      redissonClient.<String>getBucket(key).set(value, ttl);
    } catch (Exception exception) {
      // Redis is an optimization layer; request handling must continue when it is unavailable.
    }
  }
}
