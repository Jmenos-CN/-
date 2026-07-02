package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;

class RedissonAdvisorTaskQueueTest {

  @Test
  void publishesPollsAndAcknowledgesTaskMessages() {
    RedissonClient redissonClient = mock(RedissonClient.class);
    RStream<String, String> stream = mock(RStream.class);
    when(redissonClient.<String, String>getStream("advisor:tasks", StringCodec.INSTANCE))
        .thenReturn(stream);

    StreamMessageId messageId = new StreamMessageId(1700000000000L, 0L);
    when(stream.readGroup(
        eq("advisor-task-workers"),
        eq("worker-1"),
        any(StreamReadGroupArgs.class)
    )).thenReturn(Map.of(messageId, Map.of("taskId", "task-1")));

    RedissonAdvisorTaskQueue queue = new RedissonAdvisorTaskQueue(
        redissonClient,
        new AdvisorTaskStreamProperties(
            "advisor:tasks",
            "advisor-task-workers",
            "worker-1",
            Duration.ofMillis(100)
        )
    );

    queue.publish("task-1");
    var message = queue.poll();
    queue.ack(message.orElseThrow().messageId());

    assertThat(message).isPresent();
    assertThat(message.get().messageId()).isEqualTo(messageId.toString());
    assertThat(message.get().taskId()).isEqualTo("task-1");
    verify(stream).createGroup(any(StreamCreateGroupArgs.class));
    verify(stream).add(any(StreamAddArgs.class));
    verify(stream).ack("advisor-task-workers", messageId);
  }
}
