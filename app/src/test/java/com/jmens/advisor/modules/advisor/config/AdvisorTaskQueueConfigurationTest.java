package com.jmens.advisor.modules.advisor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jmens.advisor.modules.advisor.service.AdvisorTaskQueue;
import com.jmens.advisor.modules.advisor.service.InMemoryAdvisorTaskQueue;
import com.jmens.advisor.modules.advisor.service.RedissonAdvisorTaskQueue;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AdvisorTaskQueueConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(AdvisorTaskQueueConfiguration.class);

  @Test
  void registersInMemoryQueueWhenRedissonIsAbsent() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(AdvisorTaskQueue.class);
      assertThat(context.getBean(AdvisorTaskQueue.class)).isInstanceOf(InMemoryAdvisorTaskQueue.class);
    });
  }

  @Test
  void registersRedissonQueueWhenRedissonClientExists() {
    RStream<String, String> stream = mock(RStream.class);
    RedissonClient redissonClient = mock(RedissonClient.class);
    when(redissonClient.<String, String>getStream(anyString())).thenReturn(stream);

    contextRunner
        .withBean(RedissonClient.class, () -> redissonClient)
        .run(context -> {
          assertThat(context).hasSingleBean(AdvisorTaskQueue.class);
          assertThat(context.getBean(AdvisorTaskQueue.class)).isInstanceOf(RedissonAdvisorTaskQueue.class);
        });
  }
}
