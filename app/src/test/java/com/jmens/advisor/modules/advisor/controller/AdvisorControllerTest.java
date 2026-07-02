package com.jmens.advisor.modules.advisor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jmens.advisor.modules.advisor.domain.AdvisorReportSummary;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.service.AdvisorAnalysisService;
import com.jmens.advisor.modules.advisor.service.AgentRole;
import com.jmens.advisor.modules.advisor.service.AdvisorReportService;
import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import com.jmens.advisor.modules.advisor.service.ComplianceGuard;
import com.jmens.advisor.modules.advisor.service.SingleAgentAnalysis;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockSymbolParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        context -> new SingleAgentAnalysis(AgentRole.FUNDAMENTAL, "基本面稳定")
    ));
    AdvisorAnalysisService analysisService = new AdvisorAnalysisService(
        new StockSymbolParser(),
        new StubStockDataPort(),
        workflowService,
        new ComplianceGuard(),
        new CapturingAdvisorReportService()
    );
    mockMvc = MockMvcBuilders.standaloneSetup(
        new AdvisorController(analysisService, new StubAdvisorReportService())
    ).build();
  }

  @Test
  void analyzeReturnsSuccess() throws Exception {
    mockMvc.perform(post("/api/advisor/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"帮我分析600519\",\"analysisType\":\"full\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stockCode").value("600519"))
        .andExpect(jsonPath("$.data.stockName").value("贵州茅台"))
        .andExpect(jsonPath("$.data.quoteSummary").isNotEmpty())
        .andExpect(jsonPath("$.data.conclusion").value(org.hamcrest.Matchers.containsString("不构成投资建议")));
  }

  @Test
  void returnsReportDetailById() throws Exception {
    mockMvc.perform(get("/api/advisor/reports/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.stockCode").value("600519"))
        .andExpect(jsonPath("$.data.stockName").value("贵州茅台"));
  }

  @Test
  void returnsRecentReportsByStockCode() throws Exception {
    mockMvc.perform(get("/api/advisor/reports").param("stockCode", "600519"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
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
          "贵州茅台",
          "最新价 1510.00，涨跌幅 1.34%",
          LocalDateTime.of(2026, 7, 1, 10, 30)
      ));
    }

    private ResearchReport sampleReport() {
      return new ResearchReport(
          "600519",
          "贵州茅台",
          LocalDateTime.of(2026, 7, 1, 10, 30),
          "最新价 1510.00，涨跌幅 1.34%",
          "基本面稳定",
          "技术面震荡",
          "估值数据不足",
          "新闻数据暂缺",
          "需关注波动风险，不构成投资建议",
          "综合分析仅供投研参考，不构成投资建议。",
          List.of(new DataEvidence(
              "Sina Finance",
              "贵州茅台实时行情",
              "最新价 1510.00",
              LocalDateTime.of(2026, 7, 1, 10, 30)
          ))
      );
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
      return List.of();
    }
  }
}
