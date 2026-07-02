package com.jmens.advisor.modules.advisor.config;

import com.jmens.advisor.modules.advisor.service.AdvisorTaskQueue;
import com.jmens.advisor.modules.advisor.service.AdvisorTaskStreamProperties;
import com.jmens.advisor.modules.advisor.service.InMemoryAdvisorTaskQueue;
import com.jmens.advisor.modules.advisor.service.RedissonAdvisorTaskQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdvisorTaskStreamProperties.class)
public class AdvisorTaskQueueConfiguration {

  @Bean
  @ConditionalOnBean(RedissonClient.class)
  AdvisorTaskQueue redissonAdvisorTaskQueue(
      RedissonClient redissonClient,
      AdvisorTaskStreamProperties properties
  ) {
    return new RedissonAdvisorTaskQueue(redissonClient, properties);
  }

  @Bean
  @ConditionalOnMissingBean(AdvisorTaskQueue.class)
  AdvisorTaskQueue inMemoryAdvisorTaskQueue() {
    return new InMemoryAdvisorTaskQueue();
  }
}
