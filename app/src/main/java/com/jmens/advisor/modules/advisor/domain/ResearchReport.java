package com.jmens.advisor.modules.advisor.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ResearchReport(
    String stockCode,
    String stockName,
    LocalDateTime analysisTime,
    String quoteSummary,
    String fundamentalView,
    String technicalView,
    String valuationView,
    String newsView,
    String riskView,
    String conclusion,
    List<DataEvidence> evidences,
    List<ReportInsight> insights,
    ReportQuality quality
) {

  public ResearchReport {
    evidences = evidences == null ? List.of() : List.copyOf(evidences);
    insights = insights == null ? List.of() : List.copyOf(insights);
    quality = quality == null ? ReportQuality.empty() : quality;
  }

  public ResearchReport(
      String stockCode,
      String stockName,
      LocalDateTime analysisTime,
      String quoteSummary,
      String fundamentalView,
      String technicalView,
      String valuationView,
      String newsView,
      String riskView,
      String conclusion,
      List<DataEvidence> evidences
  ) {
    this(
        stockCode,
        stockName,
        analysisTime,
        quoteSummary,
        fundamentalView,
        technicalView,
        valuationView,
        newsView,
        riskView,
        conclusion,
        evidences,
        List.of(),
        ReportQuality.empty()
    );
  }
}
