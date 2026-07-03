package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportInsightServiceTest {

  private final ReportInsightService service = new ReportInsightService();

  @Test
  void mapsEvidenceSourcesToExplainableInsights() {
    List<ReportInsight> insights = service.createInsights(List.of(
        evidence("Sina Finance", "Realtime quote", "price=1510"),
        evidence("Eastmoney Financial", "Latest financial indicators", "roe=10.57"),
        evidence("Basic Valuation", "PE/PB/ROE snapshot", "PE=69.39"),
        evidence("Peer Comparison", "Peer valuation comparison", "peerMedianPE=26.67"),
        evidence("Sina Finance News", "Channel inventory stayed stable", "https://example.com/news")
    ));

    assertThat(insights).extracting(ReportInsight::type)
        .containsExactly("MARKET", "FINANCIAL", "VALUATION", "PEER", "NEWS");
    assertThat(insights).anySatisfy(insight -> {
      assertThat(insight.type()).isEqualTo("PEER");
      assertThat(insight.title()).contains("Peer");
      assertThat(insight.supportingEvidence()).contains("Peer valuation comparison");
      assertThat(insight.confidence()).isEqualByComparingTo("0.80");
    });
  }

  @Test
  void ignoresUnknownEvidenceInsteadOfCreatingMisleadingInsights() {
    List<ReportInsight> insights = service.createInsights(List.of(
        evidence("Unknown Blog", "Unverified opinion", "unknown")
    ));

    assertThat(insights).isEmpty();
  }

  private DataEvidence evidence(String source, String title, String value) {
    return new DataEvidence(source, title, value, LocalDateTime.of(2026, 7, 3, 10, 0));
  }
}
