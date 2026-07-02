package com.jmens.advisor.modules.advisor.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AdvisorAgentConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(AdvisorAgentConfiguration.class);

  @Test
  void registersFiveLangChain4jAgentRunnersWhenLlmIsEnabled() {
    contextRunner
        .withPropertyValues(
            "advisor.llm.enabled=true",
            "advisor.llm.api-key=test-key"
        )
        .run(context -> {
          assertThat(context).hasSingleBean(dev.langchain4j.model.chat.ChatModel.class);
          assertThat(context.getBeansOfType(AdvisorWorkflowService.AgentRunner.class)).hasSize(5);
        });
  }

  @Test
  void doesNotRegisterAgentRunnersByDefault() {
    contextRunner.run(context -> {
      assertThat(context).doesNotHaveBean(dev.langchain4j.model.chat.ChatModel.class);
      assertThat(context.getBeansOfType(AdvisorWorkflowService.AgentRunner.class)).isEmpty();
    });
  }
}
