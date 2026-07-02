package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AdvisorTaskServiceTest {

  @Autowired
  private AdvisorTaskService taskService;

  @Autowired
  private AdvisorTaskQueue taskQueue;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void drainQueue() {
    Optional<AdvisorTaskMessage> message;
    do {
      message = taskQueue.poll();
    } while (message.isPresent());
  }

  @Test
  void createsPendingTaskAndPublishesMessage() {
    AdvisorTaskResponse created = taskService.createTask("帮我分析600519", "full");

    AdvisorTaskResponse saved = taskService.getTask(created.taskId());
    assertThat(saved.status()).isEqualTo(AdvisorTaskStatus.PENDING);
    assertThat(saved.query()).isEqualTo("帮我分析600519");
    assertThat(taskQueue.poll()).hasValueSatisfying(message ->
        assertThat(message.taskId()).isEqualTo(created.taskId()));
  }

  @Test
  void publishesMessageOnlyAfterTransactionCommits() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    AtomicReference<AdvisorTaskResponse> created = new AtomicReference<>();

    transactionTemplate.executeWithoutResult(status -> {
      created.set(taskService.createTask("帮我分析600519", "full"));

      // The worker must not observe the task before the database commit is visible.
      assertThat(taskQueue.poll()).isEmpty();
    });

    assertThat(taskQueue.poll()).hasValueSatisfying(message ->
        assertThat(message.taskId()).isEqualTo(created.get().taskId()));
  }
}
