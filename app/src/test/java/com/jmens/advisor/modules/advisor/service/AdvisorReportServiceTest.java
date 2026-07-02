package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
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
    assertThat(saved.stockName()).isEqualTo("贵州茅台");
    assertThat(saved.fundamentalView()).isEqualTo("基本面稳定");
    assertThat(saved.evidences()).singleElement()
        .satisfies(evidence -> assertThat(evidence.source()).isEqualTo("Sina Finance"));
  }

  @Test
  void findsRecentReportsByStockCode() {
    reportService.save(sampleReport());
    reportService.save(new ResearchReport(
        "000001",
        "平安银行",
        LocalDateTime.of(2026, 7, 2, 11, 0),
        "最新价 10.00，涨跌幅 0.10%",
        "基本面正常",
        "技术面震荡",
        "估值中性",
        "新闻数据暂缺",
        "需关注波动风险，不构成投资建议",
        "综合分析仅供投研参考，不构成投资建议。",
        List.of()
    ));

    List<AdvisorReportSummary> reports = reportService.findRecentReports("600519");

    assertThat(reports).singleElement()
        .satisfies(summary -> {
          assertThat(summary.stockCode()).isEqualTo("600519");
          assertThat(summary.stockName()).isEqualTo("贵州茅台");
          assertThat(summary.quoteSummary()).contains("最新价 1200.00");
        });
  }

  private ResearchReport sampleReport() {
    return new ResearchReport(
        "600519",
        "贵州茅台",
        LocalDateTime.of(2026, 7, 2, 10, 0),
        "最新价 1200.00，涨跌幅 0.50%",
        "基本面稳定",
        "技术面震荡",
        "估值数据不足",
        "新闻数据暂缺",
        "需关注波动风险，不构成投资建议",
        "综合分析仅供投研参考，不构成投资建议。",
        List.of(new DataEvidence(
            "Sina Finance",
            "贵州茅台实时行情",
            "最新价 1200.00",
            LocalDateTime.of(2026, 7, 2, 10, 0)
        ))
    );
  }
}
