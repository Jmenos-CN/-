package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportQualityServiceTest {

  private final ReportQualityService service = new ReportQualityService();

  @Test
  void returnsFullScoreWhenAllCoreEvidenceTypesArePresent() {
    ReportQuality quality = service.evaluate(List.of(
        evidence("Sina Finance"),
        evidence("Eastmoney Financial"),
        evidence("Basic Valuation"),
        evidence("Peer Comparison"),
        evidence("Sina Finance News")
    ));

    assertThat(quality.qualityScore()).isEqualTo(100);
    assertThat(quality.missingEvidenceTypes()).isEmpty();
    assertThat(quality.warnings()).isEmpty();
  }

  @Test
  void reportsMissingEvidenceTypesAndWarnings() {
    ReportQuality quality = service.evaluate(List.of(
        evidence("Sina Finance"),
        evidence("Eastmoney Financial"),
        evidence("Basic Valuation")
    ));

    assertThat(quality.qualityScore()).isEqualTo(60);
    assertThat(quality.missingEvidenceTypes()).containsExactly("PEER", "NEWS");
    assertThat(quality.warnings())
        .contains("Missing PEER evidence: peer comparison was unavailable.")
        .contains("Missing NEWS evidence: recent news was unavailable.");
  }

  @Test
  void treatsNullEvidenceAsEmptyLowQualityReport() {
    ReportQuality quality = service.evaluate(null);

    assertThat(quality.qualityScore()).isZero();
    assertThat(quality.missingEvidenceTypes())
        .containsExactly("MARKET", "FINANCIAL", "VALUATION", "PEER", "NEWS");
    assertThat(quality.warnings()).contains("Report evidence is incomplete; treat conclusions as low confidence.");
  }

  private DataEvidence evidence(String source) {
    return new DataEvidence(source, source + " title", source + " value", LocalDateTime.of(2026, 7, 3, 10, 0));
  }
}
