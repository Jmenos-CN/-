package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockFinancialPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PeerComparisonService {

  private static final int MAX_PEERS = 5;
  private static final BigDecimal NEAR_THRESHOLD = new BigDecimal("0.10");

  private final PeerGroupService peerGroupService;
  private final StockDataPort stockDataPort;
  private final StockFinancialPort stockFinancialPort;
  private final BasicValuationService basicValuationService;

  public PeerComparisonService(
      PeerGroupService peerGroupService,
      StockDataPort stockDataPort,
      StockFinancialPort stockFinancialPort,
      BasicValuationService basicValuationService
  ) {
    this.peerGroupService = peerGroupService;
    this.stockDataPort = stockDataPort;
    this.stockFinancialPort = stockFinancialPort;
    this.basicValuationService = basicValuationService;
  }

  public PeerComparison compare(
      StockSymbol symbol,
      StockQuote quote,
      StockFinancialSnapshot financial
  ) {
    PeerGroupService.PeerGroup group = peerGroupService.findGroup(symbol);
    if (!group.available() || group.peerCodes().isEmpty()) {
      return unavailable("Peer comparison: not available because no configured peer group was found.");
    }
    BasicValuationService.BasicValuation current = basicValuationService.evaluate(quote, financial);
    List<PeerMetrics> peerMetrics = group.peerCodes().stream()
        .limit(MAX_PEERS)
        .map(this::loadPeerMetrics)
        .flatMap(List::stream)
        .toList();
    if (peerMetrics.isEmpty()) {
      return unavailable("Peer comparison: not available because peer data could not be loaded.");
    }

    BigDecimal peerMedianPe = median(peerMetrics.stream().map(PeerMetrics::pe).toList());
    BigDecimal peerMedianPb = median(peerMetrics.stream().map(PeerMetrics::pb).toList());
    BigDecimal peerMedianRoe = median(peerMetrics.stream().map(PeerMetrics::roe).toList());
    String text = "Peer comparison: group=%s, peerCount=%d, currentPE=%s, peerMedianPE=%s (%s), currentPB=%s, peerMedianPB=%s (%s), currentROE=%s%%, peerMedianROE=%s%% (%s). Static peer comparison only; not a forecast or investment advice."
        .formatted(
            group.name(),
            peerMetrics.size(),
            display(current.pe()),
            display(peerMedianPe),
            relation("PE", current.pe(), peerMedianPe),
            display(current.pb()),
            display(peerMedianPb),
            relation("PB", current.pb(), peerMedianPb),
            display(financial == null ? null : financial.roe()),
            display(peerMedianRoe),
            relation("ROE", financial == null ? null : financial.roe(), peerMedianRoe)
        );
    String evidence = "group=%s, peerCount=%d, currentPE=%s, peerMedianPE=%s, currentPB=%s, peerMedianPB=%s, currentROE=%s%%, peerMedianROE=%s%%"
        .formatted(
            group.name(),
            peerMetrics.size(),
            display(current.pe()),
            display(peerMedianPe),
            display(current.pb()),
            display(peerMedianPb),
            display(financial == null ? null : financial.roe()),
            display(peerMedianRoe)
        );
    return new PeerComparison(
        true,
        group.name(),
        peerMetrics.size(),
        current.pe(),
        peerMedianPe,
        current.pb(),
        peerMedianPb,
        financial == null ? null : financial.roe(),
        peerMedianRoe,
        text,
        evidence
    );
  }

  private List<PeerMetrics> loadPeerMetrics(String code) {
    try {
      StockSymbol peerSymbol = new StockSymbol(code, code.startsWith("6") ? "SH" : "SZ");
      StockQuote peerQuote = stockDataPort.getRealtimeQuote(peerSymbol);
      StockFinancialSnapshot peerFinancial = stockFinancialPort.getLatestSnapshot(peerSymbol);
      BasicValuationService.BasicValuation peerValuation =
          basicValuationService.evaluate(peerQuote, peerFinancial);
      return List.of(new PeerMetrics(
          peerValuation.pe(),
          peerValuation.pb(),
          peerFinancial.roe()
      ));
    } catch (RuntimeException ignored) {
      return List.of();
    }
  }

  private PeerComparison unavailable(String text) {
    return new PeerComparison(false, "", 0, null, null, null, null, null, null, text, text);
  }

  private BigDecimal median(List<BigDecimal> rawValues) {
    List<BigDecimal> values = rawValues.stream()
        .filter(value -> value != null)
        .sorted(Comparator.naturalOrder())
        .toList();
    if (values.isEmpty()) {
      return null;
    }
    int middle = values.size() / 2;
    if (values.size() % 2 == 1) {
      return values.get(middle);
    }
    return values.get(middle - 1)
        .add(values.get(middle))
        .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
  }

  private String relation(String metric, BigDecimal current, BigDecimal median) {
    if (current == null || median == null || median.compareTo(BigDecimal.ZERO) == 0) {
      return metric + " comparison unavailable";
    }
    BigDecimal diffRatio = current.subtract(median)
        .abs()
        .divide(median.abs(), 4, RoundingMode.HALF_UP);
    if (diffRatio.compareTo(NEAR_THRESHOLD) <= 0) {
      return metric + " is near peers";
    }
    return current.compareTo(median) > 0
        ? metric + " is higher than peers"
        : metric + " is lower than peers";
  }

  private String display(BigDecimal value) {
    if (value == null) {
      return "n/a";
    }
    return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }

  private record PeerMetrics(BigDecimal pe, BigDecimal pb, BigDecimal roe) {}

  public record PeerComparison(
      boolean available,
      String groupName,
      int peerCount,
      BigDecimal currentPe,
      BigDecimal peerMedianPe,
      BigDecimal currentPb,
      BigDecimal peerMedianPb,
      BigDecimal currentRoe,
      BigDecimal peerMedianRoe,
      String text,
      String evidenceValue
  ) {}
}
