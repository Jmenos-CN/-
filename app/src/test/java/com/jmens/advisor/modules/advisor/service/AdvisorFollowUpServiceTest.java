package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.FollowUpResponse;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import dev.langchain4j.model.chat.ChatModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvisorFollowUpServiceTest {

  @Test
  void answersWithLangChain4jUsingReportContextAndEvidence() {
    ChatModel chatModel = mock(ChatModel.class);
    when(chatModel.chat(contains("贵州茅台实时行情"))).thenReturn("需要重点关注估值与价格波动风险。");
    AdvisorFollowUpService service = new AdvisorFollowUpService(
        new StubAdvisorReportService(),
        Optional.of(chatModel)
    );

    FollowUpResponse response = service.answer(7L, "这只股票最大的风险是什么？");

    assertThat(response.reportId()).isEqualTo(7L);
    assertThat(response.question()).isEqualTo("这只股票最大的风险是什么？");
    assertThat(response.answer()).contains("价格波动风险");
    assertThat(response.citedEvidence()).containsExactly("贵州茅台实时行情");
    assertThat(response.contextSources()).contains("report:7", "evidence:Sina Finance");
    assertThat(response.llmEnabled()).isTrue();
    verify(chatModel).chat(contains("基本面稳定"));
    verify(chatModel).chat(contains("贵州茅台实时行情"));
  }

  @Test
  void returnsExplainableFallbackWhenLlmIsDisabled() {
    AdvisorFollowUpService service = new AdvisorFollowUpService(
        new StubAdvisorReportService(),
        Optional.empty()
    );

    FollowUpResponse response = service.answer(7L, "还能买吗？");

    assertThat(response.llmEnabled()).isFalse();
    assertThat(response.answer()).contains("LLM 未启用");
    assertThat(response.answer()).contains("贵州茅台实时行情");
    assertThat(response.citedEvidence()).containsExactly("贵州茅台实时行情");
  }

  @Test
  void returnsFallbackWhenLangChain4jCallFails() {
    ChatModel chatModel = mock(ChatModel.class);
    when(chatModel.chat(contains("还能买吗？"))).thenThrow(new RuntimeException("quota exhausted"));
    AdvisorFollowUpService service = new AdvisorFollowUpService(
        new StubAdvisorReportService(),
        Optional.of(chatModel)
    );

    FollowUpResponse response = service.answer(7L, "还能买吗？");

    assertThat(response.llmEnabled()).isFalse();
    assertThat(response.answer()).contains("LLM 调用失败");
    assertThat(response.answer()).contains("贵州茅台实时行情");
    assertThat(response.citedEvidence()).containsExactly("贵州茅台实时行情");
  }

  private static class StubAdvisorReportService extends AdvisorReportService {

    StubAdvisorReportService() {
      super(null, null);
    }

    @Override
    public ResearchReport getReport(Long id) {
      return new ResearchReport(
          "600519",
          "贵州茅台",
          LocalDateTime.of(2026, 7, 1, 10, 30),
          "最新价 1510.00，涨跌幅 1.34%",
          "基本面稳定",
          "技术面短期波动",
          "估值处于偏高区间",
          "新闻热度较高",
          "需要关注价格波动和估值回撤风险",
          "仅供研究参考，不构成投资建议。",
          List.of(new DataEvidence(
              "Sina Finance",
              "贵州茅台实时行情",
              "最新价 1510.00",
              LocalDateTime.of(2026, 7, 1, 10, 30)
          )),
          List.of(new ReportInsight(
              "RISK",
              "风险提示",
              "价格波动和估值回撤是主要观察点。",
              List.of("贵州茅台实时行情"),
              "MEDIUM",
              new BigDecimal("0.80")
          )),
          new ReportQuality(80, List.of(), List.of())
      );
    }
  }
}
