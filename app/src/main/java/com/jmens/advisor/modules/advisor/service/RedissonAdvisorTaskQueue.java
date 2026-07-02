package com.jmens.advisor.modules.advisor.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;

/**
 * Redis Stream implementation of advisor task delivery.
 *
 * <p>The database remains the source of truth for task state. Redis Stream only
 * carries task IDs so failed application restarts do not lose the authoritative
 * task record.</p>
 */
public class RedissonAdvisorTaskQueue implements AdvisorTaskQueue {

  private static final String TASK_ID_FIELD = "taskId";

  private final RStream<String, String> stream;
  private final AdvisorTaskStreamProperties properties;
  private final Map<String, StreamMessageId> deliveredMessages = new ConcurrentHashMap<>();

  public RedissonAdvisorTaskQueue(
      RedissonClient redissonClient,
      AdvisorTaskStreamProperties properties
  ) {
    this.stream = redissonClient.getStream(properties.name());
    this.properties = properties;
    ensureConsumerGroup();
  }

  @Override
  public void publish(String taskId) {
    stream.add(StreamAddArgs.entry(TASK_ID_FIELD, taskId));
  }

  @Override
  public Optional<AdvisorTaskMessage> poll() {
    Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
        properties.group(),
        properties.consumer(),
        StreamReadGroupArgs.neverDelivered()
            .count(1)
            .timeout(properties.pollTimeout())
    );
    if (messages == null || messages.isEmpty()) {
      return Optional.empty();
    }
    var entry = messages.entrySet().iterator().next();
    String taskId = entry.getValue().get(TASK_ID_FIELD);
    if (taskId == null || taskId.isBlank()) {
      return Optional.empty();
    }
    String messageId = entry.getKey().toString();
    deliveredMessages.put(messageId, entry.getKey());
    return Optional.of(new AdvisorTaskMessage(messageId, taskId));
  }

  @Override
  public void ack(String messageId) {
    StreamMessageId streamMessageId = deliveredMessages.remove(messageId);
    if (streamMessageId != null) {
      stream.ack(properties.group(), streamMessageId);
    }
  }

  private void ensureConsumerGroup() {
    try {
      stream.createGroup(StreamCreateGroupArgs.name(properties.group()).makeStream());
    } catch (RuntimeException ex) {
      if (!isConsumerGroupAlreadyExists(ex)) {
        throw ex;
      }
    }
  }

  private boolean isConsumerGroupAlreadyExists(RuntimeException ex) {
    String message = ex.getMessage();
    return message != null && message.toUpperCase(Locale.ROOT).contains("BUSYGROUP");
  }
}
