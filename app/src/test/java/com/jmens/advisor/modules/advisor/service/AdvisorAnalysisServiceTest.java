package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockSymbolParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvisorAnalysisServiceTest {

  @Test
  void analyzesQueryByParsingSymbolFetchingQuoteAndRunningAgentsWithContext() {
    StockDataPort stockDataPort = new StubStockDataPort();
    CapturingAdvisorReportService reportService = new CapturingAdvisorReportService();
    AdvisorWorkflowService workflowService = new AdvisorWorkflowService(List.of(
        context -> {
          assertThat(context).contains("600519", "贵州茅台", "1510.00", "1.34%");
          assertThat(context).contains("KLine summary", "latestClose=1193.01", "high=1210.00", "low=1166.33");
          assertThat(context).contains("financial/news data not configured");
          return new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳定");
        },
        context -> new SingleAgentAnalysis(AgentRole.RISK, "不存在确定性收益，需要关注波动风险")
    ));
    AdvisorAnalysisService service = new AdvisorAnalysisService(
        new StockSymbolParser(),
        stockDataPort,
        workflowService,
        new ComplianceGuard(),
        reportService,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, null, null, null)
    );

    ResearchReport report = service.analyze("帮我分析600519", "full");

    assertThat(report.stockCode()).isEqualTo("600519");
    assertThat(report.stockName()).isEqualTo("贵州茅台");
    assertThat(report.quoteSummary()).contains("最新价 1510.00", "涨跌幅 1.34%");
    assertThat(report.fundamentalView()).isEqualTo("基本面稳定");
    assertThat(report.riskView()).contains("波动风险");
    assertThat(report.conclusion()).contains("不构成投资建议");
    assertThat(report.evidences()).singleElement()
        .satisfies(evidence -> {
          assertThat(evidence.source()).isEqualTo("Sina Finance");
          assertThat(evidence.value()).contains("1510.00");
        });
    assertThat(reportService.savedReport.stockCode()).isEqualTo("600519");
    assertThat(reportService.savedReport.conclusion()).contains("不构成投资建议");
  }

  @Test
  void returnsCachedReportForRepeatedRequestWithoutRunningExpensiveChainAgain() {
    CountingStockDataPort stockDataPort = new CountingStockDataPort();
    CountingAdvisorReportService reportService = new CountingAdvisorReportService();
    CountingAgentRunner agentRunner = new CountingAgentRunner();
    AdvisorAnalysisService service = new AdvisorAnalysisService(
        new StockSymbolParser(),
        stockDataPort,
        new AdvisorWorkflowService(List.of(agentRunner)),
        new ComplianceGuard(),
        reportService,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, null, null, null)
    );

    ResearchReport first = service.analyze("帮我分析600519", "full");
    ResearchReport second = service.analyze("600519", "full");

    assertThat(first.stockCode()).isEqualTo("600519");
    assertThat(second.stockCode()).isEqualTo("600519");
    assertThat(stockDataPort.quoteCalls).isEqualTo(1);
    assertThat(stockDataPort.klineCalls).isEqualTo(1);
    assertThat(agentRunner.calls).isEqualTo(1);
    assertThat(reportService.saveCalls).isEqualTo(1);
  }

  private static class CapturingAdvisorReportService extends AdvisorReportService {

    private ResearchReport savedReport;

    CapturingAdvisorReportService() {
      super(null, null);
    }

    @Override
    public Long save(ResearchReport report) {
      this.savedReport = report;
      return 1L;
    }
  }

  private static class CountingAdvisorReportService extends CapturingAdvisorReportService {

    private int saveCalls;

    @Override
    public Long save(ResearchReport report) {
      saveCalls++;
      return super.save(report);
    }
  }

  private static class CountingAgentRunner implements AdvisorWorkflowService.AgentRunner {

    private int calls;

    @Override
    public SingleAgentAnalysis run(String stockCode) {
      calls++;
      return new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳定");
    }
  }

  private static class CountingStockDataPort extends StubStockDataPort {

    private int quoteCalls;
    private int klineCalls;

    @Override
    public StockQuote getRealtimeQuote(StockSymbol symbol) {
      quoteCalls++;
      return super.getRealtimeQuote(symbol);
    }

    @Override
    public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
      klineCalls++;
      return super.getRecentKLine(symbol, days);
    }
  }

  private static class StubStockDataPort implements StockDataPort {

    @Override
    public StockQuote getRealtimeQuote(StockSymbol symbol) {
      return new StockQuote(
          symbol.code(),
          "贵州茅台",
          new BigDecimal("1510.00"),
          new BigDecimal("1490.00"),
          new BigDecimal("1.34"),
          123456L,
          new BigDecimal("185000000.00"),
          LocalDateTime.of(2026, 7, 1, 10, 30)
      );
    }

    @Override
    public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
      return List.of(
          new KLinePoint(
              LocalDate.of(2026, 6, 30),
              new BigDecimal("1187.00"),
              new BigDecimal("1185.49"),
              new BigDecimal("1195.67"),
              new BigDecimal("1176.00"),
              3960779L
          ),
          new KLinePoint(
              LocalDate.of(2026, 7, 1),
              new BigDecimal("1180.10"),
              new BigDecimal("1193.01"),
              new BigDecimal("1210.00"),
              new BigDecimal("1166.33"),
              4247381L
          )
      );
    }
  }
}
