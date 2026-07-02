package com.jmens.advisor.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

class RedissonCacheClientTest {

  @Test
  void returnsCacheMissWhenRedisReadFails() {
    RedissonClient redissonClient = mock(RedissonClient.class);
    @SuppressWarnings("unchecked")
    RBucket<String> bucket = mock(RBucket.class);
    when(redissonClient.<String>getBucket("stock:quote:600519")).thenReturn(bucket);
    when(bucket.get()).thenThrow(new IllegalStateException("redis down"));
    RedissonCacheClient cacheClient = new RedissonCacheClient(redissonClient);

    assertThat(cacheClient.get("stock:quote:600519")).isEmpty();
  }

  @Test
  void ignoresRedisWriteFailure() {
    RedissonClient redissonClient = mock(RedissonClient.class);
    @SuppressWarnings("unchecked")
    RBucket<String> bucket = mock(RBucket.class);
    when(redissonClient.<String>getBucket("stock:quote:600519")).thenReturn(bucket);
    org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
        .when(bucket).set("{}", Duration.ofSeconds(5));
    RedissonCacheClient cacheClient = new RedissonCacheClient(redissonClient);

    cacheClient.put("stock:quote:600519", "{}", Duration.ofSeconds(5));
  }
}
