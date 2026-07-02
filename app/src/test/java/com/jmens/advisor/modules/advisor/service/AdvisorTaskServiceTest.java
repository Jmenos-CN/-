package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AdvisorTaskServiceTest {

  @Autowired
  private AdvisorTaskService taskService;

  @Autowired
  private AdvisorTaskQueue taskQueue;

  @Test
  void createsPendingTaskAndPublishesMessage() {
    AdvisorTaskResponse created = taskService.createTask("帮我分析600519", "full");

    AdvisorTaskResponse saved = taskService.getTask(created.taskId());
    assertThat(saved.status()).isEqualTo(AdvisorTaskStatus.PENDING);
    assertThat(saved.query()).isEqualTo("帮我分析600519");
    assertThat(taskQueue.poll()).hasValueSatisfying(message ->
        assertThat(message.taskId()).isEqualTo(created.taskId()));
  }
}
