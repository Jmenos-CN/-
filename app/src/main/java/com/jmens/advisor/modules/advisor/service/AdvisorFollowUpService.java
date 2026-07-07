package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.FollowUpResponse;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import dev.langchain4j.model.chat.ChatModel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Answers a user follow-up question using one saved report as the grounding context.
 */
@Service
public class AdvisorFollowUpService {

  private final AdvisorReportService reportService;
  private final Optional<ChatModel> chatModel;

  public AdvisorFollowUpService(AdvisorReportService reportService, Optional<ChatModel> chatModel) {
    this.reportService = reportService;
    this.chatModel = chatModel == null ? Optional.empty() : chatModel;
  }

  /**
   * Builds a report-scoped prompt and asks LangChain4j to answer the follow-up.
   *
   * @param reportId saved report ID
   * @param question user follow-up question
   * @return grounded answer and the evidence titles used as context
   */
  public FollowUpResponse answer(Long reportId, String question) {
    ResearchReport report = reportService.getReport(reportId);
    List<String> citedEvidence = report.evidences().stream()
        .map(DataEvidence::title)
        .filter(title -> title != null && !title.isBlank())
        .toList();
    List<String> contextSources = buildContextSources(reportId, report);
    String answer = chatModel
        .map(model -> model.chat(buildPrompt(report, question)))
        .orElseGet(() -> fallbackAnswer(report, citedEvidence));
    return new FollowUpResponse(
        reportId,
        question,
        answer,
        citedEvidence,
        contextSources,
        chatModel.isPresent(),
        LocalDateTime.now()
    );
  }

  private List<String> buildContextSources(Long reportId, ResearchReport report) {
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of("report:" + reportId),
            report.evidences().stream()
                .map(evidence -> "evidence:" + evidence.source())
        )
        .distinct()
        .toList();
  }

  private String buildPrompt(ResearchReport report, String question) {
    return """
        你是一个谨慎的 A 股投研助手。只能基于给定报告回答用户追问，不得编造报告外数据。
        如果报告证据不足，请明确说明不足。回答必须包含风险提示，且不得构成投资建议。

        用户追问:
        %s

        报告股票:
        %s %s

        行情摘要:
        %s

        基本面分析:
        %s

        技术面分析:
        %s

        估值分析:
        %s

        新闻分析:
        %s

        风险分析:
        %s

        总结:
        %s

        证据:
        %s
        """.formatted(
        question,
        report.stockCode(),
        report.stockName(),
        report.quoteSummary(),
        report.fundamentalView(),
        report.technicalView(),
        report.valuationView(),
        report.newsView(),
        report.riskView(),
        report.conclusion(),
        formatEvidence(report.evidences())
    );
  }

  private String formatEvidence(List<DataEvidence> evidences) {
    if (evidences.isEmpty()) {
      return "无可用证据";
    }
    return evidences.stream()
        .map(evidence -> "- [%s] %s: %s".formatted(
            evidence.source(),
            evidence.title(),
            evidence.value()
        ))
        .reduce((left, right) -> left + "\n" + right)
        .orElse("无可用证据");
  }

  private String fallbackAnswer(ResearchReport report, List<String> citedEvidence) {
    String evidenceSummary = citedEvidence.isEmpty()
        ? "当前报告没有可引用证据"
        : "可引用证据包括：" + String.join("、", citedEvidence);
    return "LLM 未启用，无法生成自然语言追问回答。"
        + "你仍可以先参考当前报告的风险段落："
        + report.riskView()
        + "。"
        + evidenceSummary
        + "。以上内容仅供研究演示，不构成投资建议。";
  }
}
