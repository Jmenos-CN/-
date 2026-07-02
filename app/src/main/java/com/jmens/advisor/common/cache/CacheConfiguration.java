package com.jmens.advisor.common.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Owns cache wiring so Redis can stay optional for local development and tests.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AdvisorRedisProperties.class, CacheTtlProperties.class})
public class CacheConfiguration {

  @Bean(destroyMethod = "shutdown")
  @ConditionalOnProperty(prefix = "app.cache.redis", name = "enabled", havingValue = "true")
  RedissonClient advisorRedissonClient(AdvisorRedisProperties properties) {
    Config config = new Config();
    config.useSingleServer()
        .setAddress(properties.address())
        .setDatabase(properties.getDatabase());
    return Redisson.create(config);
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.cache.redis", name = "enabled", havingValue = "true")
  CacheClient redissonCacheClient(RedissonClient advisorRedissonClient) {
    return new RedissonCacheClient(advisorRedissonClient);
  }

  @Bean
  @ConditionalOnMissingBean(CacheClient.class)
  CacheClient noopCacheClient() {
    return new NoopCacheClient();
  }

  @Bean
  JsonCacheService jsonCacheService(CacheClient cacheClient) {
    return new JsonCacheService(cacheClient);
  }
}
