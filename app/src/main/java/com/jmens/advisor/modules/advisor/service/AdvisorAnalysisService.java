package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.common.cache.CacheKey;
import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ResearchReport;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockSymbolParser;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdvisorAnalysisService {

  private final StockSymbolParser stockSymbolParser;
  private final StockDataPort stockDataPort;
  private final AdvisorWorkflowService advisorWorkflowService;
  private final ComplianceGuard complianceGuard;
  private final AdvisorReportService advisorReportService;
  private final JsonCacheService cacheService;
  private final CacheTtlProperties ttlProperties;

  public AdvisorAnalysisService(
      StockSymbolParser stockSymbolParser,
      StockDataPort stockDataPort,
      AdvisorWorkflowService advisorWorkflowService,
      ComplianceGuard complianceGuard,
      AdvisorReportService advisorReportService,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this.stockSymbolParser = stockSymbolParser;
    this.stockDataPort = stockDataPort;
    this.advisorWorkflowService = advisorWorkflowService;
    this.complianceGuard = complianceGuard;
    this.advisorReportService = advisorReportService;
    this.cacheService = cacheService;
    this.ttlProperties = ttlProperties;
  }

  public ResearchReport analyze(String query, String analysisType) {
    StockSymbol symbol = stockSymbolParser.parse(query);
    String normalizedAnalysisType = normalizeAnalysisType(analysisType);
    String reportCacheKey = CacheKey.report(symbol.code(), normalizedAnalysisType);
    var cachedReport = cacheService.get(reportCacheKey, ResearchReport.class);
    if (cachedReport.isPresent()) {
      return cachedReport.get();
    }
    StockQuote quote = stockDataPort.getRealtimeQuote(symbol);
    String agentContext = buildAgentContext(query, normalizedAnalysisType, quote);
    List<SingleAgentAnalysis> analyses = advisorWorkflowService.runAgents(agentContext);
    Map<AgentRole, String> byRole = toRoleMap(analyses);
    String quoteSummary = buildQuoteSummary(quote);
    String conclusion = complianceGuard.sanitize(buildConclusion(quote, analyses));

    ResearchReport report = new ResearchReport(
        quote.code(),
        quote.name(),
        LocalDateTime.now(),
        quoteSummary,
        byRole.getOrDefault(AgentRole.FUNDAMENTAL, ""),
        byRole.getOrDefault(AgentRole.TECHNICAL, ""),
        byRole.getOrDefault(AgentRole.VALUATION, ""),
        byRole.getOrDefault(AgentRole.NEWS, ""),
        byRole.getOrDefault(AgentRole.RISK, ""),
        conclusion,
        List.of(new DataEvidence(
            "Sina Finance",
            quote.name() + "实时行情",
            quoteSummary,
            quote.quoteTime()
        ))
    );
    advisorReportService.save(report);
    cacheService.put(reportCacheKey, report, ttlProperties.report());
    return report;
  }

  private String normalizeAnalysisType(String analysisType) {
    return analysisType == null || analysisType.isBlank() ? "full" : analysisType;
  }

  private String buildAgentContext(String query, String analysisType, StockQuote quote) {
    return """
        用户问题: %s
        分析类型: %s
        股票代码: %s
        股票名称: %s
        最新价: %s
        昨收: %s
        涨跌幅: %s%%
        成交量: %d
        成交额: %s
        行情时间: %s

        请基于上述真实行情上下文进行投研分析，禁止输出确定性买卖建议。
        """.formatted(
        query,
        analysisType,
        quote.code(),
        quote.name(),
        quote.latestPrice(),
        quote.previousClose(),
        quote.changePercent(),
        quote.volume(),
        quote.amount(),
        quote.quoteTime()
    );
  }

  private Map<AgentRole, String> toRoleMap(List<SingleAgentAnalysis> analyses) {
    Map<AgentRole, String> byRole = new EnumMap<>(AgentRole.class);
    for (SingleAgentAnalysis analysis : analyses) {
      byRole.put(analysis.role(), analysis.content());
    }
    return byRole;
  }

  private String buildQuoteSummary(StockQuote quote) {
    return "最新价 %s，昨收 %s，涨跌幅 %s%%，成交量 %d，成交额 %s，行情时间 %s"
        .formatted(
            quote.latestPrice(),
            quote.previousClose(),
            quote.changePercent(),
            quote.volume(),
            quote.amount(),
            quote.quoteTime()
        );
  }

  private String buildConclusion(StockQuote quote, List<SingleAgentAnalysis> analyses) {
    String agentSummary = analyses.stream()
        .map(analysis -> analysis.role() + ": " + analysis.content())
        .reduce((left, right) -> left + "\n" + right)
        .orElse("当前未配置分析 Agent，仅返回行情摘要。");
    return """
        %s(%s) 当前行情摘要：%s。
        Agent 分析摘要：
        %s
        """.formatted(quote.name(), quote.code(), buildQuoteSummary(quote), agentSummary);
  }
}
