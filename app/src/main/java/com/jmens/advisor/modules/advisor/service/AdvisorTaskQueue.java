package com.jmens.advisor.modules.advisor.service;

import java.util.Optional;

public interface AdvisorTaskQueue {

  void publish(String taskId);

  Optional<AdvisorTaskMessage> poll();

  void ack(String messageId);
}
