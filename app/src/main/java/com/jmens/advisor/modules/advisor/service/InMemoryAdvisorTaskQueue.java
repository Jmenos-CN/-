package com.jmens.advisor.modules.advisor.service;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryAdvisorTaskQueue implements AdvisorTaskQueue {

  private final Queue<AdvisorTaskMessage> messages = new ConcurrentLinkedQueue<>();

  @Override
  public void publish(String taskId) {
    messages.add(new AdvisorTaskMessage(UUID.randomUUID().toString(), taskId));
  }

  @Override
  public Optional<AdvisorTaskMessage> poll() {
    return Optional.ofNullable(messages.poll());
  }

  @Override
  public void ack(String messageId) {
  }
}
