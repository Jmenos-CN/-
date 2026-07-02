package com.jmens.advisor.modules.advisor.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drains advisor analysis tasks when async consumption is enabled.
 */
@Component
@ConditionalOnProperty(prefix = "app.advisor.tasks.consumer", name = "enabled", havingValue = "true")
public class AdvisorTaskConsumerScheduler {

  private final AdvisorTaskWorker worker;

  public AdvisorTaskConsumerScheduler(AdvisorTaskWorker worker) {
    this.worker = worker;
  }

  @Scheduled(fixedDelayString = "${app.advisor.tasks.consumer.fixed-delay:1000}")
  public void pollOnce() {
    worker.processNext();
  }
}
