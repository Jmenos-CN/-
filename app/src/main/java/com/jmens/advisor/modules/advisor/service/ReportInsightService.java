package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Builds deterministic explanation cards from collected report evidence.
 */
@Service
public class ReportInsightService {

  private static final Map<String, InsightTemplate> TEMPLATES = Map.of(
      "Sina Finance", new InsightTemplate(
          "MARKET",
          "Market quote evidence",
          "Realtime quote data anchors the report to observable market movement.",
          "LOW",
          "0.90"
      ),
      "Eastmoney Financial", new InsightTemplate(
          "FINANCIAL",
          "Financial indicator evidence",
          "Financial statement indicators provide the basis for profitability and balance-sheet comments.",
          "MEDIUM",
          "0.85"
      ),
      "Basic Valuation", new InsightTemplate(
          "VALUATION",
          "Basic valuation evidence",
          "PE, PB, and ROE explain whether the current price looks expensive or cheap against fundamentals.",
          "MEDIUM",
          "0.80"
      ),
      "Peer Comparison", new InsightTemplate(
          "PEER",
          "Peer comparison evidence",
          "Industry peer medians add a relative valuation baseline instead of relying on one stock alone.",
          "MEDIUM",
          "0.80"
      ),
      "Sina Finance News", new InsightTemplate(
          "NEWS",
          "News evidence",
          "Recent news provides event context that may explain short-term sentiment or risk.",
          "HIGH",
          "0.70"
      )
  );

  /**
   * Converts known evidence sources into stable report insights.
   *
   * @param evidences evidence entries collected during report generation
   * @return ordered insights for known evidence categories
   */
  public List<ReportInsight> createInsights(List<DataEvidence> evidences) {
    if (evidences == null || evidences.isEmpty()) {
      return List.of();
    }
    return evidences.stream()
        .filter(Objects::nonNull)
        .map(this::toInsight)
        .filter(Objects::nonNull)
        .toList();
  }

  private ReportInsight toInsight(DataEvidence evidence) {
    InsightTemplate template = TEMPLATES.get(evidence.source());
    if (template == null) {
      return null;
    }
    return new ReportInsight(
        template.type(),
        template.title(),
        template.summary(),
        List.of(evidence.title() == null ? template.title() : evidence.title()),
        template.riskLevel(),
        new BigDecimal(template.confidence())
    );
  }

  private record InsightTemplate(
      String type,
      String title,
      String summary,
      String riskLevel,
      String confidence
  ) {}
}
