package com.jmens.advisor.modules.advisor.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Explains one report conclusion category with the evidence titles that support it.
 *
 * @param type stable insight category, such as MARKET, FINANCIAL, VALUATION, PEER, or NEWS
 * @param title human-readable explanation title
 * @param summary concise explanation of why this evidence matters
 * @param supportingEvidence evidence titles used to produce the insight
 * @param riskLevel coarse risk label for interview/demo readability
 * @param confidence deterministic confidence derived from evidence source quality
 */
public record ReportInsight(
    String type,
    String title,
    String summary,
    List<String> supportingEvidence,
    String riskLevel,
    BigDecimal confidence
) {

  public ReportInsight {
    supportingEvidence = supportingEvidence == null ? List.of() : List.copyOf(supportingEvidence);
    confidence = confidence == null ? BigDecimal.ZERO : confidence;
  }
}
