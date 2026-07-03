package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Scores whether a report has enough independently collected evidence to support its narrative.
 */
@Service
public class ReportQualityService {

  private static final Map<String, String> REQUIRED_SOURCES = new LinkedHashMap<>();
  private static final Map<String, String> MISSING_WARNINGS = new LinkedHashMap<>();

  static {
    REQUIRED_SOURCES.put("MARKET", "Sina Finance");
    REQUIRED_SOURCES.put("FINANCIAL", "Eastmoney Financial");
    REQUIRED_SOURCES.put("VALUATION", "Basic Valuation");
    REQUIRED_SOURCES.put("PEER", "Peer Comparison");
    REQUIRED_SOURCES.put("NEWS", "Sina Finance News");

    MISSING_WARNINGS.put("MARKET", "Missing MARKET evidence: realtime quote was unavailable.");
    MISSING_WARNINGS.put("FINANCIAL", "Missing FINANCIAL evidence: financial indicators were unavailable.");
    MISSING_WARNINGS.put("VALUATION", "Missing VALUATION evidence: PE/PB/ROE valuation was unavailable.");
    MISSING_WARNINGS.put("PEER", "Missing PEER evidence: peer comparison was unavailable.");
    MISSING_WARNINGS.put("NEWS", "Missing NEWS evidence: recent news was unavailable.");
  }

  /**
   * Computes report evidence quality from required evidence categories.
   *
   * @param evidences evidence entries collected during report generation
   * @return completeness score and missing-data warnings
   */
  public ReportQuality evaluate(List<DataEvidence> evidences) {
    Set<String> sources = evidences == null ? Set.of() : evidences.stream()
        .filter(Objects::nonNull)
        .map(DataEvidence::source)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<String> missingTypes = REQUIRED_SOURCES.entrySet().stream()
        .filter(entry -> !sources.contains(entry.getValue()))
        .map(Map.Entry::getKey)
        .toList();
    int presentCount = REQUIRED_SOURCES.size() - missingTypes.size();
    int score = presentCount * 100 / REQUIRED_SOURCES.size();
    List<String> warnings = buildWarnings(missingTypes);
    return new ReportQuality(score, missingTypes, warnings);
  }

  private List<String> buildWarnings(List<String> missingTypes) {
    if (missingTypes.isEmpty()) {
      return List.of();
    }
    List<String> warnings = missingTypes.stream()
        .map(MISSING_WARNINGS::get)
        .toList();
    if (missingTypes.size() >= 3) {
      return java.util.stream.Stream.concat(
          java.util.stream.Stream.of("Report evidence is incomplete; treat conclusions as low confidence."),
          warnings.stream()
      ).toList();
    }
    return warnings;
  }
}
