package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.common.cache.CacheKey;
import com.jmens.advisor.common.cache.CacheTtlProperties;
import com.jmens.advisor.common.cache.JsonCacheService;
import com.jmens.advisor.modules.advisor.domain.DataEvidence;
import com.jmens.advisor.modules.advisor.domain.ReportInsight;
import com.jmens.advisor.modules.advisor.domain.ReportQuality;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdvisorAnalysisService {

  private static final int KLINE_DAYS = 20;
  private static final int NEWS_LIMIT = 5;

  private final StockSymbolParser stockSymbolParser;
  private final StockDataPort stockDataPort;
  private final StockNewsPort stockNewsPort;
  private final StockFinancialPort stockFinancialPort;
  private final BasicValuationService basicValuationService;
  private final PeerComparisonService peerComparisonService;
  private final ReportInsightService reportInsightService;
  private final ReportQualityService reportQualityService;
  private final AdvisorWorkflowService advisorWorkflowService;
  private final ComplianceGuard complianceGuard;
  private final AdvisorReportService advisorReportService;
  private final JsonCacheService cacheService;
  private final CacheTtlProperties ttlProperties;

  public AdvisorAnalysisService(
      StockSymbolParser stockSymbolParser,
      StockDataPort stockDataPort,
      StockNewsPort stockNewsPort,
      StockFinancialPort stockFinancialPort,
      BasicValuationService basicValuationService,
      PeerComparisonService peerComparisonService,
      ReportInsightService reportInsightService,
      ReportQualityService reportQualityService,
      AdvisorWorkflowService advisorWorkflowService,
      ComplianceGuard complianceGuard,
      AdvisorReportService advisorReportService,
      JsonCacheService cacheService,
      CacheTtlProperties ttlProperties
  ) {
    this.stockSymbolParser = stockSymbolParser;
    this.stockDataPort = stockDataPort;
    this.stockNewsPort = stockNewsPort;
    this.stockFinancialPort = stockFinancialPort;
    this.basicValuationService = basicValuationService;
    this.peerComparisonService = peerComparisonService;
    this.reportInsightService = reportInsightService;
    this.reportQualityService = reportQualityService;
    this.advisorWorkflowService = advisorWorkflowService;
    this.complianceGuard = complianceGuard;
    this.advisorReportService = advisorReportService;
    this.cacheService = cacheService;
    this.ttlProperties = ttlProperties;
  }

  /**
   * Generates a stock research report from realtime quote, recent K-line data, and Agent outputs.
   *
   * @param query user request containing a stock code
   * @param analysisType requested analysis depth or category
   * @return generated report snapshot
   */
  public ResearchReport analyze(String query, String analysisType) {
    StockSymbol symbol = stockSymbolParser.parse(query);
    String normalizedAnalysisType = normalizeAnalysisType(analysisType);
    String reportCacheKey = CacheKey.report(symbol.code(), normalizedAnalysisType);
    var cachedReport = cacheService.get(reportCacheKey, ResearchReport.class);
    if (cachedReport.isPresent()) {
      return cachedReport.get();
    }
    StockQuote quote = stockDataPort.getRealtimeQuote(symbol);
    List<KLinePoint> kLines = fetchKLines(symbol);
    List<StockNewsItem> news = fetchNews(symbol);
    StockFinancialSnapshot financial = fetchFinancial(symbol);
    String kLineSummary = buildKLineSummary(kLines);
    String newsSummary = buildNewsSummary(news);
    String financialSummary = buildFinancialSummary(financial);
    BasicValuationService.BasicValuation basicValuation = basicValuationService.evaluate(quote, financial);
    String valuationSummary = buildValuationSummary(basicValuation);
    PeerComparisonService.PeerComparison peerComparison = peerComparisonService.compare(symbol, quote, financial);
    String peerComparisonSummary = buildPeerComparisonSummary(peerComparison);
    String agentContext = buildAgentContext(
        query,
        normalizedAnalysisType,
        quote,
        kLineSummary,
        newsSummary,
        financialSummary,
        valuationSummary,
        peerComparisonSummary
    );
    List<SingleAgentAnalysis> analyses = advisorWorkflowService.runAgents(agentContext);
    Map<AgentRole, String> byRole = toRoleMap(analyses);
    String quoteSummary = buildQuoteSummary(quote);
    String conclusion = complianceGuard.sanitize(buildConclusion(quote, analyses));
    List<DataEvidence> evidences = buildEvidences(quote, quoteSummary, news, financial, basicValuation, peerComparison);
    List<ReportInsight> insights = reportInsightService.createInsights(evidences);
    ReportQuality quality = reportQualityService.evaluate(evidences);

    ResearchReport report = new ResearchReport(
        quote.code(),
        quote.name(),
        LocalDateTime.now(),
        quoteSummary,
        byRole.getOrDefault(AgentRole.FUNDAMENTAL, ""),
        byRole.getOrDefault(AgentRole.TECHNICAL, ""),
        valueOrFallback(byRole.get(AgentRole.VALUATION), buildFallbackValuationView(basicValuation, peerComparison)),
        byRole.getOrDefault(AgentRole.NEWS, ""),
        byRole.getOrDefault(AgentRole.RISK, ""),
        conclusion,
        evidences,
        insights,
        quality
    );
    advisorReportService.save(report);
    cacheService.put(reportCacheKey, report, ttlProperties.report());
    return report;
  }

  private String normalizeAnalysisType(String analysisType) {
    return analysisType == null || analysisType.isBlank() ? "full" : analysisType;
  }

  private List<KLinePoint> fetchKLines(StockSymbol symbol) {
    try {
      return stockDataPort.getRecentKLine(symbol, KLINE_DAYS);
    } catch (RuntimeException ignored) {
      // K-line is enrichment data; quote-based analysis should still work when the public endpoint is unstable.
      return List.of();
    }
  }

  private List<StockNewsItem> fetchNews(StockSymbol symbol) {
    try {
      return stockNewsPort.getRecentNews(symbol, NEWS_LIMIT);
    } catch (RuntimeException ignored) {
      // News is enrichment data; report generation should continue when the public page is unavailable.
      return List.of();
    }
  }

  private StockFinancialSnapshot fetchFinancial(StockSymbol symbol) {
    try {
      return stockFinancialPort.getLatestSnapshot(symbol);
    } catch (RuntimeException ignored) {
      // Financial metrics enrich the prompt; report generation should continue when the public endpoint is unstable.
      return null;
    }
  }

  private String buildAgentContext(
      String query,
      String analysisType,
      StockQuote quote,
      String kLineSummary,
      String newsSummary,
      String financialSummary,
      String valuationSummary,
      String peerComparisonSummary
  ) {
    return """
        User query: %s
        Analysis type: %s
        Stock code: %s
        Stock name: %s
        Latest price: %s
        Previous close: %s
        Change percent: %s%%
        Volume: %d
        Amount: %s
        Quote time: %s
        %s
        %s
        %s
        %s
        %s

        Please produce evidence-based research only. Do not fabricate facts that are not present in the context.
        Do not output deterministic buy/sell instructions.
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
        quote.quoteTime(),
        kLineSummary,
        newsSummary,
        financialSummary,
        valuationSummary,
        peerComparisonSummary
    );
  }

  private String buildKLineSummary(List<KLinePoint> kLines) {
    if (kLines.isEmpty()) {
      return "KLine summary: not available";
    }
    KLinePoint latest = kLines.get(kLines.size() - 1);
    BigDecimal high = kLines.stream()
        .map(KLinePoint::high)
        .max(Comparator.naturalOrder())
        .orElse(latest.high());
    BigDecimal low = kLines.stream()
        .map(KLinePoint::low)
        .min(Comparator.naturalOrder())
        .orElse(latest.low());
    return "KLine summary: days=%d, latestDate=%s, latestClose=%s, high=%s, low=%s, latestVolume=%d"
        .formatted(
            kLines.size(),
            latest.tradeDate(),
            latest.close(),
            high,
            low,
            latest.volume()
        );
  }

  private String buildNewsSummary(List<StockNewsItem> news) {
    if (news.isEmpty()) {
      return "News summary: not available";
    }
    return "News summary:\n" + news.stream()
        .map(item -> "- [%s] %s%s (%s)".formatted(
            item.publishedAt(),
            item.title(),
            newsSummarySuffix(item),
            item.url()
        ))
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
  }

  private String newsSummarySuffix(StockNewsItem item) {
    if (item.summary() == null || item.summary().isBlank()) {
      return "";
    }
    return " - summary: " + item.summary();
  }

  private String buildFinancialSummary(StockFinancialSnapshot financial) {
    if (financial == null) {
      return "Financial summary: not available. financial data not configured: do not fabricate revenue, profit, valuation, or policy facts.";
    }
    return "Financial summary: reportDate=%s, reportType=%s, eps=%s, bps=%s, revenue=%s, parentNetProfit=%s, roe=%s, debtRatio=%s"
        .formatted(
            financial.reportDate(),
            financial.reportType(),
            format(financial.eps(), 2),
            format(financial.bps(), 2),
            format(financial.totalOperatingRevenue(), 2),
            format(financial.parentNetProfit(), 2),
            format(financial.roe(), 2),
            format(financial.debtRatio(), 2)
        );
  }

  private String buildFinancialEvidenceValue(StockFinancialSnapshot financial) {
    return "reportDate=%s, reportType=%s, eps=%s, bps=%s, revenue=%s, parentNetProfit=%s, roe=%s, debtRatio=%s"
        .formatted(
            financial.reportDate(),
            financial.reportType(),
            format(financial.eps(), 2),
            format(financial.bps(), 2),
            format(financial.totalOperatingRevenue(), 2),
            format(financial.parentNetProfit(), 2),
            format(financial.roe(), 2),
            format(financial.debtRatio(), 2)
        );
  }

  private String buildValuationSummary(BasicValuationService.BasicValuation valuation) {
    return "Valuation summary: " + valuation.text();
  }

  private String buildPeerComparisonSummary(PeerComparisonService.PeerComparison peerComparison) {
    return "Peer comparison summary: " + peerComparison.text();
  }

  private String buildFallbackValuationView(
      BasicValuationService.BasicValuation basicValuation,
      PeerComparisonService.PeerComparison peerComparison
  ) {
    return basicValuation.text() + "\n" + peerComparison.text();
  }

  private String valueOrFallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String format(BigDecimal value, int scale) {
    if (value == null) {
      return "n/a";
    }
    return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }

  private List<DataEvidence> buildEvidences(
      StockQuote quote,
      String quoteSummary,
      List<StockNewsItem> news,
      StockFinancialSnapshot financial,
      BasicValuationService.BasicValuation basicValuation,
      PeerComparisonService.PeerComparison peerComparison
  ) {
    List<DataEvidence> evidences = new ArrayList<>();
    evidences.add(new DataEvidence(
        "Sina Finance",
        quote.name() + " realtime quote",
        quoteSummary,
        quote.quoteTime()
    ));
    for (StockNewsItem item : news) {
      evidences.add(new DataEvidence(
          "Sina Finance News",
          item.title(),
          item.url(),
          item.publishedAt()
      ));
    }
    if (financial != null) {
      evidences.add(new DataEvidence(
          "Eastmoney Financial",
          financial.stockName() + " latest financial indicators",
          buildFinancialEvidenceValue(financial),
          financial.reportDate() == null ? LocalDateTime.now() : financial.reportDate().atStartOfDay()
      ));
    }
    if (basicValuation != null && basicValuation.available()) {
      evidences.add(new DataEvidence(
          "Basic Valuation",
          quote.name() + " PE/PB/ROE snapshot",
          basicValuation.evidenceValue(),
          quote.quoteTime()
      ));
    }
    if (peerComparison != null && peerComparison.available()) {
      evidences.add(new DataEvidence(
          "Peer Comparison",
          quote.name() + " peer valuation comparison",
          peerComparison.evidenceValue(),
          quote.quoteTime()
      ));
    }
    return evidences;
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
