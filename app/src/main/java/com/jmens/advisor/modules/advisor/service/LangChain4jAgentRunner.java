package com.jmens.advisor.modules.advisor.service;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Executes one advisor role by injecting the prepared market context into a LangChain4j prompt.
 */
public class LangChain4jAgentRunner implements AdvisorWorkflowService.AgentRunner {

  private final AgentRole role;
  private final ChatModel chatModel;
  private final String promptTemplate;

  public LangChain4jAgentRunner(AgentRole role, ChatModel chatModel, String promptTemplate) {
    this.role = role;
    this.chatModel = chatModel;
    this.promptTemplate = promptTemplate;
  }

  @Override
  public SingleAgentAnalysis run(String agentContext) {
    String prompt = promptTemplate
        .replace("{context}", agentContext)
        .replace("{stockCode}", agentContext);
    return new SingleAgentAnalysis(role, chatModel.chat(prompt));
  }
}
