package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

class LangChain4jAgentRunnerTest {

  @Test
  void returnsAgentAnalysisFromChatModel() {
    ChatModel chatModel = mock(ChatModel.class);
    when(chatModel.chat(contains("600519"))).thenReturn("基本面分析结果");
    LangChain4jAgentRunner runner = new LangChain4jAgentRunner(
        AgentRole.FUNDAMENTAL,
        chatModel,
        "请分析股票 {stockCode}"
    );

    SingleAgentAnalysis analysis = runner.run("600519");

    assertThat(analysis.role()).isEqualTo(AgentRole.FUNDAMENTAL);
    assertThat(analysis.content()).isEqualTo("基本面分析结果");
  }
}
