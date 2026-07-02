package com.jmens.advisor.modules.advisor.config;

import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import com.jmens.advisor.modules.advisor.service.AgentRole;
import com.jmens.advisor.modules.advisor.service.LangChain4jAgentRunner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * Wires real LangChain4j-backed advisor agents when external LLM access is explicitly enabled.
 *
 * <p>The configuration is conditional because local tests and ordinary startup must remain usable
 * without paid API credentials or network access.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdvisorLlmProperties.class)
@ConditionalOnProperty(prefix = "advisor.llm", name = "enabled", havingValue = "true")
public class AdvisorAgentConfiguration {

  /**
   * Creates an OpenAI-compatible chat model for DashScope or any compatible provider.
   *
   * @param properties typed LLM configuration loaded from environment-backed application settings
   * @return shared chat model used by all role-specific advisor agents
   * @throws IllegalStateException when LLM mode is enabled without an API key
   */
  @Bean
  ChatModel advisorChatModel(AdvisorLlmProperties properties) {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new IllegalStateException("advisor.llm.api-key must be configured when advisor.llm.enabled=true");
    }
    return OpenAiChatModel.builder()
        .baseUrl(properties.getBaseUrl())
        .apiKey(properties.getApiKey())
        .modelName(properties.getModelName())
        .temperature(properties.getTemperature())
        .timeout(properties.getTimeout())
        .build();
  }

  @Bean
  AdvisorWorkflowService.AgentRunner fundamentalAgentRunner(
      ChatModel advisorChatModel,
      ResourceLoader resourceLoader
  ) {
    return runner(AgentRole.FUNDAMENTAL, advisorChatModel, resourceLoader, "fundamental-agent.st");
  }

  @Bean
  AdvisorWorkflowService.AgentRunner technicalAgentRunner(
      ChatModel advisorChatModel,
      ResourceLoader resourceLoader
  ) {
    return runner(AgentRole.TECHNICAL, advisorChatModel, resourceLoader, "technical-agent.st");
  }

  @Bean
  AdvisorWorkflowService.AgentRunner valuationAgentRunner(
      ChatModel advisorChatModel,
      ResourceLoader resourceLoader
  ) {
    return runner(AgentRole.VALUATION, advisorChatModel, resourceLoader, "valuation-agent.st");
  }

  @Bean
  AdvisorWorkflowService.AgentRunner newsAgentRunner(
      ChatModel advisorChatModel,
      ResourceLoader resourceLoader
  ) {
    return runner(AgentRole.NEWS, advisorChatModel, resourceLoader, "news-agent.st");
  }

  @Bean
  AdvisorWorkflowService.AgentRunner riskAgentRunner(
      ChatModel advisorChatModel,
      ResourceLoader resourceLoader
  ) {
    return runner(AgentRole.RISK, advisorChatModel, resourceLoader, "risk-agent.st");
  }

  /**
   * Creates a role-specific LangChain4j runner from a classpath prompt template.
   *
   * @param role advisor role that owns the generated analysis section
   * @param chatModel shared LangChain4j chat model
   * @param resourceLoader Spring resource loader used to read prompt files from packaged jars
   * @param promptFile classpath prompt file name under {@code prompts/}
   * @return executable advisor runner
   */
  private LangChain4jAgentRunner runner(
      AgentRole role,
      ChatModel chatModel,
      ResourceLoader resourceLoader,
      String promptFile
  ) {
    return new LangChain4jAgentRunner(role, chatModel, readPrompt(resourceLoader, promptFile));
  }

  private String readPrompt(ResourceLoader resourceLoader, String promptFile) {
    Resource resource = resourceLoader.getResource("classpath:prompts/" + promptFile);
    try {
      return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("Failed to read advisor prompt template: " + promptFile, exception);
    }
  }
}
