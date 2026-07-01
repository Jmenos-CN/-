package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdvisorWorkflowServiceTest {

  @Test
  void aggregatesFiveAgentViewsIntoReportDraft() {
    AdvisorWorkflowService service = new AdvisorWorkflowService(List.of(
        request -> new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳定"),
        request -> new SingleAgentAnalysis(AgentRole.TECHNICAL, "技术面震荡"),
        request -> new SingleAgentAnalysis(AgentRole.VALUATION, "估值中性"),
        request -> new SingleAgentAnalysis(AgentRole.NEWS, "新闻偏中性"),
        request -> new SingleAgentAnalysis(AgentRole.RISK, "存在波动风险")
    ));

    List<SingleAgentAnalysis> analyses = service.runAgents("600519");

    assertThat(analyses).hasSize(5);
    assertThat(analyses).extracting(SingleAgentAnalysis::role)
        .containsExactlyInAnyOrder(
            AgentRole.FUNDAMENTAL,
            AgentRole.TECHNICAL,
            AgentRole.VALUATION,
            AgentRole.NEWS,
            AgentRole.RISK
        );
  }
}
