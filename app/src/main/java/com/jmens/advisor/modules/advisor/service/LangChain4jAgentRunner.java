package com.jmens.advisor.modules.advisor.service;

import dev.langchain4j.model.chat.ChatModel;

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
  public SingleAgentAnalysis run(String stockCode) {
    String prompt = promptTemplate.replace("{stockCode}", stockCode);
    return new SingleAgentAnalysis(role, chatModel.chat(prompt));
  }
}
