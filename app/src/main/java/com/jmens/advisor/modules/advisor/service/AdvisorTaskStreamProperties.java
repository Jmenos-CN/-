package com.jmens.advisor.modules.advisor.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Stream settings for advisor async analysis tasks.
 *
 * @param name stream key used to carry task IDs
 * @param group Redis consumer group name
 * @param consumer consumer name for the current application instance
 * @param pollTimeout max wait time for one blocking stream read
 */
@ConfigurationProperties(prefix = "app.advisor.tasks.stream")
public record AdvisorTaskStreamProperties(
    String name,
    String group,
    String consumer,
    Duration pollTimeout
) {

  public AdvisorTaskStreamProperties {
    name = defaultIfBlank(name, "advisor:tasks");
    group = defaultIfBlank(group, "advisor-task-workers");
    consumer = defaultIfBlank(consumer, "local-consumer");
    pollTimeout = pollTimeout == null ? Duration.ofSeconds(1) : pollTimeout;
  }

  private static String defaultIfBlank(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
