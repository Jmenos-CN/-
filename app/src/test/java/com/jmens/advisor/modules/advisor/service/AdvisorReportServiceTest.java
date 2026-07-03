package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AdvisorReportServiceTest {

  @Autowired
  private AdvisorReportService reportService;

  @Test
  void savesAndReadsReportDetail() {
    ResearchReport report = sampleReport();

    Long id = reportService.save(report);

    ResearchReport saved = reportService.getReport(id);
    assertThat(saved.stockCode()).isEqualTo("600519");
    assertThat(saved.stockName()).isEqualTo("Kweichow Moutai");
    assertThat(saved.fundamentalView()).isEqualTo("Fundamentals are stable");
    assertThat(saved.evidences()).singleElement()
        .satisfies(evidence -> assertThat(evidence.source()).isEqualTo("Sina Finance"));
    assertThat(saved.insights()).singleElement()
        .satisfies(insight -> assertThat(insight.type()).isEqualTo("MARKET"));
    assertThat(saved.quality().qualityScore()).isEqualTo(20);
    assertThat(saved.quality().missingEvidenceTypes()).contains("FINANCIAL", "VALUATION", "PEER", "NEWS");
  }

  @Test
  void findsRecentReportsByStockCode() {
    reportService.save(sampleReport());
    reportService.save(new ResearchReport(
        "000001",
        "Ping An Bank",
        LocalDateTime.of(2026, 7, 2, 11, 0),
        "Latest price 10.00, change 0.10%",
        "Fundamentals are normal",
        "Technical view is volatile",
        "Valuation is neutral",
        "News data unavailable",
        "Pay attention to volatility risk; not investment advice",
        "Research summary is for reference only and not investment advice.",
        List.of()
    ));

    List<AdvisorReportSummary> reports = reportService.findRecentReports("600519");

    assertThat(reports).singleElement()
        .satisfies(summary -> {
          assertThat(summary.stockCode()).isEqualTo("600519");
          assertThat(summary.stockName()).isEqualTo("Kweichow Moutai");
          assertThat(summary.quoteSummary()).contains("Latest price 1200.00");
        });
  }

  private ResearchReport sampleReport() {
    return new ResearchReport(
        "600519",
        "Kweichow Moutai",
        LocalDateTime.of(2026, 7, 2, 10, 0),
        "Latest price 1200.00, change 0.50%",
        "Fundamentals are stable",
        "Technical view is volatile",
        "Valuation data is insufficient",
        "News data unavailable",
        "Pay attention to volatility risk; not investment advice",
        "Research summary is for reference only and not investment advice.",
        List.of(new DataEvidence(
            "Sina Finance",
            "Kweichow Moutai realtime quote",
            "Latest price 1200.00",
            LocalDateTime.of(2026, 7, 2, 10, 0)
        )),
        List.of(new ReportInsight(
            "MARKET",
            "Market quote evidence",
            "Realtime quote data anchors the report to observable market movement.",
            List.of("Kweichow Moutai realtime quote"),
            "LOW",
            new BigDecimal("0.90")
        )),
        new ReportQuality(
            20,
            List.of("FINANCIAL", "VALUATION", "PEER", "NEWS"),
            List.of("Missing FINANCIAL evidence: financial indicators were unavailable.")
        )
    );
  }
}
