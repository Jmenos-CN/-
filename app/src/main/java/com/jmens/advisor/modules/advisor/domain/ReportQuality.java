package com.jmens.advisor.modules.advisor.domain;

import java.util.List;

/**
 * Describes whether a generated report has enough evidence for a reliable explanation.
 *
 * @param qualityScore evidence completeness score from 0 to 100
 * @param missingEvidenceTypes required evidence categories that were unavailable
 * @param warnings caveats that should be displayed or reviewed before trusting the report
 */
public record ReportQuality(
    int qualityScore,
    List<String> missingEvidenceTypes,
    List<String> warnings
) {

  public ReportQuality {
    missingEvidenceTypes = missingEvidenceTypes == null ? List.of() : List.copyOf(missingEvidenceTypes);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  public static ReportQuality empty() {
    return new ReportQuality(
        0,
        List.of("MARKET", "FINANCIAL", "VALUATION", "PEER", "NEWS"),
        List.of("Report evidence is incomplete; treat conclusions as low confidence.")
    );
  }
}
