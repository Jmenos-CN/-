package com.jmens.advisor.modules.advisor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.InMemoryCacheClient;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.advisor.config.PeerGroupProperties;
import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.AdvisorTaskResponse;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.FollowUpResponse;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.service.AdvisorAnalysisService;
import com.jmens.advisor.modules.advisor.service.AdvisorFollowUpService;
import com.jmens.advisor.modules.advisor.service.AdvisorReportService;
import com.jmens.advisor.modules.advisor.service.AdvisorTaskService;
import com.jmens.advisor.modules.advisor.service.AdvisorTaskStatus;
import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import com.jmens.advisor.modules.advisor.service.AgentRole;
import com.jmens.advisor.modules.advisor.service.BasicValuationService;
import com.jmens.advisor.modules.advisor.service.ComplianceGuard;
import com.jmens.advisor.modules.advisor.service.PeerComparisonService;
import com.jmens.advisor.modules.advisor.service.PeerGroupService;
import com.jmens.advisor.modules.advisor.service.ReportInsightService;
import com.jmens.advisor.modules.advisor.service.ReportQualityService;
import com.jmens.advisor.modules.advisor.service.SingleAgentAnalysis;
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
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdvisorControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AdvisorWorkflowService workflowService = new AdvisorWorkflowService(List.of(
        context -> new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "Fundamentals are stable")
    ));
    StubStockDataPort stockDataPort = new StubStockDataPort();
    StubStockFinancialPort stockFinancialPort = new StubStockFinancialPort();
    BasicValuationService basicValuationService = new BasicValuationService();
    PeerGroupService peerGroupService = new PeerGroupService(new PeerGroupProperties(Map.of()));
    AdvisorAnalysisService analysisService = new AdvisorAnalysisService(
        new StockSymbolParser(),
        stockDataPort,
        new StubStockNewsPort(),
        stockFinancialPort,
        basicValuationService,
        new PeerComparisonService(peerGroupService, stockDataPort, stockFinancialPort, basicValuationService),
        new ReportInsightService(),
        new ReportQualityService(),
        workflowService,
        new ComplianceGuard(),
        new CapturingAdvisorReportService(),
        new JsonCacheService(new InMemoryCacheClient()),
        new CacheTtlProperties(null, null, null, null, null)
    );
    mockMvc = MockMvcBuilders.standaloneSetup(
        new AdvisorController(
            analysisService,
            new StubAdvisorReportService(),
            new StubAdvisorTaskService(),
            new StubAdvisorFollowUpService()
        )
    ).build();
  }

  @Test
  void analyzeReturnsQualityInsightsAndEvidenceContract() throws Exception {
    mockMvc.perform(post("/api/advisor/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"Analyze 600519\",\"analysisType\":\"full\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stockCode").value("600519"))
        .andExpect(jsonPath("$.data.stockName").value("Kweichow Moutai"))
        .andExpect(jsonPath("$.data.quoteSummary").isNotEmpty())
        .andExpect(jsonPath("$.data.quality.qualityScore").value(80))
        .andExpect(jsonPath("$.data.quality.missingEvidenceTypes[0]").value("PEER"))
        .andExpect(jsonPath("$.data.insights[0].type").value("MARKET"))
        .andExpect(jsonPath("$.data.insights[0].supportingEvidence[0]").isNotEmpty())
        .andExpect(jsonPath("$.data.evidences[0].source").value("Sina Finance"))
        .andExpect(jsonPath("$.data.conclusion").value(Matchers.containsString("Fundamentals are stable")));
  }

  @Test
  void returnsReportDetailWithExplainabilityFieldsById() throws Exception {
    mockMvc.perform(get("/api/advisor/reports/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stockCode").value("600519"))
        .andExpect(jsonPath("$.data.stockName").value("Kweichow Moutai"))
        .andExpect(jsonPath("$.data.quality.qualityScore").value(20))
        .andExpect(jsonPath("$.data.insights[0].type").value("MARKET"))
        .andExpect(jsonPath("$.data.evidences[0].source").value("Sina Finance"));
  }

  @Test
  void returnsRecentReportsByStockCode() throws Exception {
    mockMvc.perform(get("/api/advisor/reports").param("stockCode", "600519"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
  }

  @Test
  void createsAsyncAnalysisTask() throws Exception {
    mockMvc.perform(post("/api/advisor/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"Analyze 600519\",\"analysisType\":\"full\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.taskId").value("task-1"))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void returnsAsyncAnalysisTaskById() throws Exception {
    mockMvc.perform(get("/api/advisor/tasks/task-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.taskId").value("task-1"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.reportId").value(1));
  }

  @Test
  void answersFollowUpQuestionAgainstExistingReport() throws Exception {
    mockMvc.perform(post("/api/advisor/reports/1/follow-up")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"question\":\"这只股票最大的风险是什么？\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reportId").value(1))
        .andExpect(jsonPath("$.data.question").value("这只股票最大的风险是什么？"))
        .andExpect(jsonPath("$.data.answer").value(Matchers.containsString("波动风险")))
        .andExpect(jsonPath("$.data.citedEvidence[0]").value("Kweichow Moutai realtime quote"))
        .andExpect(jsonPath("$.data.contextSources[0]").value("report:1"))
        .andExpect(jsonPath("$.data.llmEnabled").value(true));
  }

  private static class CapturingAdvisorReportService extends AdvisorReportService {

    CapturingAdvisorReportService() {
      super(null, null);
    }

    @Override
    public Long save(ResearchReport report) {
      return 1L;
    }
  }

  private static class StubAdvisorReportService extends AdvisorReportService {

    StubAdvisorReportService() {
      super(null, null);
    }

    @Override
    public ResearchReport getReport(Long id) {
      return sampleReport();
    }

    @Override
    public List<AdvisorReportSummary> findRecentReports(String stockCode) {
      return List.of(new AdvisorReportSummary(
          1L,
          stockCode,
          "Kweichow Moutai",
          "Latest price 1510.00, change 1.34%",
          LocalDateTime.of(2026, 7, 1, 10, 30)
      ));
    }

    private ResearchReport sampleReport() {
      return new ResearchReport(
          "600519",
          "Kweichow Moutai",
          LocalDateTime.of(2026, 7, 1, 10, 30),
          "Latest price 1510.00, change 1.34%",
          "Fundamentals are stable",
          "Technical view is volatile",
          "Valuation data is insufficient",
          "News data unavailable",
          "Pay attention to volatility risk; not investment advice",
          "Research summary is for reference only and not investment advice.",
          List.of(new DataEvidence(
              "Sina Finance",
              "Kweichow Moutai realtime quote",
              "Latest price 1510.00",
              LocalDateTime.of(2026, 7, 1, 10, 30)
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

  private static class StubAdvisorTaskService extends AdvisorTaskService {

    StubAdvisorTaskService() {
      super(null, null);
    }

    @Override
    public AdvisorTaskResponse createTask(String query, String analysisType) {
      return new AdvisorTaskResponse(
          "task-1",
          query,
          analysisType,
          AdvisorTaskStatus.PENDING,
          null,
          null,
          LocalDateTime.of(2026, 7, 1, 10, 30),
          LocalDateTime.of(2026, 7, 1, 10, 30)
      );
    }

    @Override
    public AdvisorTaskResponse getTask(String taskId) {
      return new AdvisorTaskResponse(
          taskId,
          "Analyze 600519",
          "full",
          AdvisorTaskStatus.COMPLETED,
          1L,
          null,
          LocalDateTime.of(2026, 7, 1, 10, 30),
          LocalDateTime.of(2026, 7, 1, 10, 31)
      );
    }
  }

  private static class StubAdvisorFollowUpService extends AdvisorFollowUpService {

    StubAdvisorFollowUpService() {
      super(null, java.util.Optional.empty());
    }

    @Override
    public FollowUpResponse answer(Long reportId, String question) {
      return new FollowUpResponse(
          reportId,
          question,
          "主要关注价格波动风险和证据覆盖不足。",
          List.of("Kweichow Moutai realtime quote"),
          List.of("report:" + reportId),
          true,
          LocalDateTime.of(2026, 7, 1, 10, 35)
      );
    }
  }

  private static class StubStockDataPort implements StockDataPort {

    @Override
    public StockQuote getRealtimeQuote(StockSymbol symbol) {
      return new StockQuote(
          symbol.code(),
          "Kweichow Moutai",
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
      return List.of();
    }
  }

  private static class StubStockNewsPort implements StockNewsPort {

    @Override
    public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
      return List.of(new StockNewsItem(
          "Kweichow Moutai news",
          "https://finance.sina.com.cn/news1.shtml",
          LocalDateTime.of(2026, 7, 2, 17, 20),
          "Sina Finance"
      ));
    }
  }

  private static class StubStockFinancialPort implements StockFinancialPort {

    @Override
    public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
      return new StockFinancialSnapshot(
          symbol.code(),
          "Kweichow Moutai",
          LocalDate.of(2026, 3, 31),
          "Q1",
          new BigDecimal("21.76"),
          new BigDecimal("216.32234994607"),
          new BigDecimal("54702912385.23"),
          new BigDecimal("27242512886.45"),
          new BigDecimal("10.57"),
          new BigDecimal("12.1227489682")
      );
    }
  }
}
