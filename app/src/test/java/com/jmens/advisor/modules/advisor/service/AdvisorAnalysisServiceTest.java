package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockFinancialPort;
import com.jmens.advisor.modules.stock.service.StockNewsPort;
import com.jmens.advisor.modules.stock.service.StockSymbolParser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AdvisorAnalysisServiceTest {

  @Test
  void analyzesQueryWithQuoteKLineNewsAndFinancialContext() {
    CapturingAdvisorReportService reportService = new CapturingAdvisorReportService();
    AdvisorWorkflowService workflowService = new AdvisorWorkflowService(List.of(
        context -> {
          assertThat(context).contains("600519", "Kweichow Moutai", "1510.00", "1.34%");
          assertThat(context).contains("KLine summary", "latestClose=1193.01", "high=1210.00", "low=1166.33");
          assertThat(context).contains("News summary", "Market Article");
          assertThat(context).contains("Financial summary", "eps=21.76", "roe=10.57", "debtRatio=12.12");
          assertThat(context).contains("Valuation summary", "PE=69.39", "PB=6.98");
          assertThat(context).contains("Peer comparison summary", "peerMedianPE=26.67");
          assertThat(context).doesNotContain("financial data not configured");
          return new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "fundamental analysis");
        },
        context -> new SingleAgentAnalysis(AgentRole.RISK, "risk analysis")
    ));
    AdvisorAnalysisService service = service(
        new StubStockDataPort(),
        new StubStockNewsPort(),
        new StubStockFinancialPort(),
        workflowService,
        reportService
    );

    ResearchReport report = service.analyze("Analyze 600519", "full");

    assertThat(report.stockCode()).isEqualTo("600519");
    assertThat(report.stockName()).isEqualTo("Kweichow Moutai");
    assertThat(report.quoteSummary()).contains("1510.00", "1.34");
    assertThat(report.fundamentalView()).isEqualTo("fundamental analysis");
    assertThat(report.valuationView()).contains("Basic valuation", "PE=69.39", "PB=6.98", "Peer comparison");
    assertThat(report.riskView()).contains("risk analysis");
    assertThat(report.conclusion()).contains("fundamental analysis", "risk analysis");
    assertThat(report.evidences()).hasSize(5);
    assertThat(report.evidences().get(0).source()).isEqualTo("Sina Finance");
    assertThat(report.evidences().get(1).source()).isEqualTo("Sina Finance News");
    assertThat(report.evidences()).anySatisfy(evidence -> {
      assertThat(evidence.source()).isEqualTo("Eastmoney Financial");
      assertThat(evidence.value()).contains("eps=21.76", "roe=10.57");
    });
    assertThat(report.evidences()).anySatisfy(evidence -> {
      assertThat(evidence.source()).isEqualTo("Basic Valuation");
      assertThat(evidence.value()).contains("PE=69.39", "PB=6.98", "ROE=10.57%");
    });
    assertThat(report.evidences()).anySatisfy(evidence -> {
      assertThat(evidence.source()).isEqualTo("Peer Comparison");
      assertThat(evidence.value()).contains("peerMedianPE=26.67", "peerMedianPB=4");
    });
    assertThat(report.insights()).extracting(insight -> insight.type())
        .containsExactly("MARKET", "NEWS", "FINANCIAL", "VALUATION", "PEER");
    assertThat(report.quality().qualityScore()).isEqualTo(100);
    assertThat(report.quality().missingEvidenceTypes()).isEmpty();
    assertThat(reportService.savedReport.stockCode()).isEqualTo("600519");
  }

  @Test
  void generatesReadableChineseQuoteSummaryAndConclusion() {
    AdvisorAnalysisService service = service(
        new StubStockDataPort(),
        new StubStockNewsPort(),
        new StubStockFinancialPort(),
        new AdvisorWorkflowService(List.of(
            context -> new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳健，收入和利润数据需要结合最新财报确认。")
        )),
        new CapturingAdvisorReportService()
    );

    ResearchReport report = service.analyze("帮我分析600519", "full");

    assertThat(report.quoteSummary())
        .contains("最新价", "昨收", "涨跌幅", "成交量", "成交额", "行情时间")
        .doesNotContain("鏈", "锛", "閸", "鐢", "璇");
    assertThat(report.conclusion())
        .contains("Kweichow Moutai", "当前行情摘要", "Agent 分析摘要", "不构成投资建议")
        .doesNotContain("鏈", "锛", "閸", "鐢", "璇");
  }

  @Test
  void returnsCachedReportForRepeatedRequestWithoutRunningExpensiveChainAgain() {
    CountingStockDataPort stockDataPort = new CountingStockDataPort();
    CountingStockNewsPort stockNewsPort = new CountingStockNewsPort();
    CountingStockFinancialPort stockFinancialPort = new CountingStockFinancialPort();
    CountingAdvisorReportService reportService = new CountingAdvisorReportService();
    CountingAgentRunner agentRunner = new CountingAgentRunner();
    AdvisorAnalysisService service = service(
        stockDataPort,
        stockNewsPort,
        stockFinancialPort,
        new AdvisorWorkflowService(List.of(agentRunner)),
        reportService
    );

    ResearchReport first = service.analyze("Analyze 600519", "full");
    ResearchReport second = service.analyze("600519", "full");

    assertThat(first.stockCode()).isEqualTo("600519");
    assertThat(second.stockCode()).isEqualTo("600519");
    assertThat(stockDataPort.quoteCalls).isEqualTo(4);
    assertThat(stockDataPort.klineCalls).isEqualTo(1);
    assertThat(stockNewsPort.calls).isEqualTo(1);
    assertThat(stockFinancialPort.calls).isEqualTo(4);
    assertThat(agentRunner.calls).isEqualTo(1);
    assertThat(reportService.saveCalls).isEqualTo(1);
  }

  @Test
  void includesNewsArticleSummaryInAgentContext() {
    AtomicReference<String> capturedContext = new AtomicReference<>();
    StockNewsPort stockNewsPort = (symbol, limit) -> List.of(new StockNewsItem(
        "Market Article",
        "https://finance.sina.com.cn/news1.shtml",
        LocalDateTime.of(2026, 7, 2, 17, 20),
        "Sina Finance",
        "Company channel inventory stayed stable."
    ));
    AdvisorAnalysisService service = service(
        new StubStockDataPort(),
        stockNewsPort,
        new StubStockFinancialPort(),
        new AdvisorWorkflowService(List.of(context -> {
          capturedContext.set(context);
          return new SingleAgentAnalysis(AgentRole.NEWS, "news analysis");
        })),
        new CapturingAdvisorReportService()
    );

    service.analyze("Analyze 600519", "full");

    assertThat(capturedContext.get())
        .contains("News summary")
        .contains("Market Article")
        .contains("Company channel inventory stayed stable.");
  }

  private AdvisorAnalysisService service(
      StockDataPort stockDataPort,
      StockNewsPort stockNewsPort,
      StockFinancialPort stockFinancialPort,
      AdvisorWorkflowService workflowService,
      AdvisorReportService reportService
  ) {
    PeerGroupService peerGroupService = new PeerGroupService(
        new com.jmens.advisor.modules.advisor.config.PeerGroupProperties(java.util.Map.of(
            "liquor", List.of("600519", "000858", "000568", "600809")
        ))
    );
    BasicValuationService basicValuationService = new BasicValuationService();
    return new AdvisorAnalysisService(
        new StockSymbolParser(),
        stockDataPort,
        stockNewsPort,
        stockFinancialPort,
        basicValuationService,
        new PeerComparisonService(
            peerGroupService,
            stockDataPort,
            stockFinancialPort,
            basicValuationService
        ),
        new ReportInsightService(),
        new ReportQualityService(),
        workflowService,
        new ComplianceGuard(),
        reportService,
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, null, null, null)
    );
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
      return new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "fundamental analysis");
    }
  }

  private static class CountingStockNewsPort extends StubStockNewsPort {

    private int calls;

    @Override
    public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
      calls++;
      return super.getRecentNews(symbol, limit);
    }
  }

  private static class CountingStockFinancialPort extends StubStockFinancialPort {

    private int calls;

    @Override
    public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
      calls++;
      return super.getLatestSnapshot(symbol);
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
      if (symbol.code().equals("000858")) {
        return quote(symbol, "120.00");
      }
      if (symbol.code().equals("000568")) {
        return quote(symbol, "80.00");
      }
      if (symbol.code().equals("600809")) {
        return quote(symbol, "90.00");
      }
      return quote(symbol, "1510.00");
    }

    private StockQuote quote(StockSymbol symbol, String latestPrice) {
      return new StockQuote(
          symbol.code(),
          "Kweichow Moutai",
          new BigDecimal(latestPrice),
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

  private static class StubStockNewsPort implements StockNewsPort {

    @Override
    public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
      return List.of(new StockNewsItem(
          "Market Article",
          "https://finance.sina.com.cn/news1.shtml",
          LocalDateTime.of(2026, 7, 2, 17, 20),
          "Sina Finance"
      ));
    }
  }

  private static class StubStockFinancialPort implements StockFinancialPort {

    @Override
    public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
      if (symbol.code().equals("000858")) {
        return financial(symbol, "4.00", "30.00", "18.00", "20.00");
      }
      if (symbol.code().equals("000568")) {
        return financial(symbol, "3.00", "20.00", "16.00", "25.00");
      }
      if (symbol.code().equals("600809")) {
        return financial(symbol, "5.00", "22.50", "22.00", "18.00");
      }
      return financial(symbol, "21.76", "216.32234994607", "10.57", "12.1227489682");
    }

    private StockFinancialSnapshot financial(
        StockSymbol symbol,
        String eps,
        String bps,
        String roe,
        String debtRatio
    ) {
      return new StockFinancialSnapshot(
          symbol.code(),
          "Kweichow Moutai",
          LocalDate.of(2026, 3, 31),
          "Q1",
          new BigDecimal(eps),
          new BigDecimal(bps),
          new BigDecimal("54702912385.23"),
          new BigDecimal("27242512886.45"),
          new BigDecimal(roe),
          new BigDecimal(debtRatio)
      );
    }
  }
}
