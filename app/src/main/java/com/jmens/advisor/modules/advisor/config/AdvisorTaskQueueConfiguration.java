package com.jmens.advisor.modules.advisor.config;

import com.jmens.advisor.modules.advisor.service.AdvisorTaskQueue;
import com.jmens.advisor.modules.advisor.service.InMemoryAdvisorTaskQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdvisorTaskQueueConfiguration {

  @Bean
  @ConditionalOnMissingBean(AdvisorTaskQueue.class)
  AdvisorTaskQueue inMemoryAdvisorTaskQueue() {
    return new InMemoryAdvisorTaskQueue();
  }
}
