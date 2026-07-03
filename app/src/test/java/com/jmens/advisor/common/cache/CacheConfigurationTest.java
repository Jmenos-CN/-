package com.jmens.advisor.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CacheConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(CacheConfiguration.class);

  @Test
  void registersNoopCacheClientByDefault() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(CacheClient.class);
      assertThat(context).hasSingleBean(JsonCacheService.class);
      assertThat(context.getBean(CacheClient.class)).isInstanceOf(NoopCacheClient.class);
      assertThat(context).doesNotHaveBean(RedissonClient.class);
    });
  }

  @Test
  void registersRedissonCacheClientWhenRedisIsEnabled() {
    contextRunner
        .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
        .withPropertyValues(
            "app.cache.redis.enabled=true",
            "app.cache.redis.host=192.168.150.101",
            "app.cache.redis.port=6379"
        )
        .run(context -> {
          assertThat(context).hasSingleBean(RedissonClient.class);
          assertThat(context.getBean(CacheClient.class)).isInstanceOf(RedissonCacheClient.class);
        });
  }
}
