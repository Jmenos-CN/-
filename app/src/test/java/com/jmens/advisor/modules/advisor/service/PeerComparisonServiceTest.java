package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.jmens.advisor.modules.stock.service.StockFinancialPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PeerComparisonServiceTest {

  private final BasicValuationService valuationService = new BasicValuationService();

  @Test
  void comparesCurrentValuationAgainstPeerMedians() {
    PeerComparisonService service = new PeerComparisonService(
        new StaticPeerGroupService(new PeerGroupService.PeerGroup(true, "liquor", List.of("000858", "000568", "600809"))),
        new StubStockDataPort(),
        new StubStockFinancialPort(),
        valuationService
    );

    PeerComparisonService.PeerComparison comparison = service.compare(
        symbol("600519"),
        quote("600519", "1510.00"),
        financial("600519", "21.76", "216.32", "10.57", "12.12")
    );

    assertThat(comparison.available()).isTrue();
    assertThat(comparison.groupName()).isEqualTo("liquor");
    assertThat(comparison.peerCount()).isEqualTo(3);
    assertThat(comparison.currentPe()).isEqualByComparingTo("69.39");
    assertThat(comparison.peerMedianPe()).isEqualByComparingTo("26.67");
    assertThat(comparison.currentPb()).isEqualByComparingTo("6.98");
    assertThat(comparison.peerMedianPb()).isEqualByComparingTo("4.00");
    assertThat(comparison.currentRoe()).isEqualByComparingTo("10.57");
    assertThat(comparison.peerMedianRoe()).isEqualByComparingTo("18.00");
    assertThat(comparison.text())
        .contains("Peer comparison")
        .contains("group=liquor")
        .contains("currentPE=69.39")
        .contains("peerMedianPE=26.67")
        .contains("PE is higher than peers")
        .contains("ROE is lower than peers");
    assertThat(comparison.evidenceValue()).contains("currentPE=69.39", "peerMedianPB=4");
  }

  @Test
  void returnsUnavailableWhenNoPeerGroupExists() {
    PeerComparisonService service = new PeerComparisonService(
        new StaticPeerGroupService(new PeerGroupService.PeerGroup(false, "", List.of())),
        new StubStockDataPort(),
        new StubStockFinancialPort(),
        valuationService
    );

    PeerComparisonService.PeerComparison comparison = service.compare(
        symbol("600519"),
        quote("600519", "1510.00"),
        financial("600519", "21.76", "216.32", "10.57", "12.12")
    );

    assertThat(comparison.available()).isFalse();
    assertThat(comparison.text()).contains("Peer comparison: not available");
  }

  private StockSymbol symbol(String code) {
    return new StockSymbol(code, code.startsWith("6") ? "SH" : "SZ");
  }

  private StockQuote quote(String code, String latestPrice) {
    return new StockQuote(
        code,
        "Stock " + code,
        new BigDecimal(latestPrice),
        new BigDecimal("100.00"),
        BigDecimal.ZERO,
        1000L,
        new BigDecimal("100000.00"),
        LocalDateTime.of(2026, 7, 1, 10, 30)
    );
  }

  private StockFinancialSnapshot financial(String code, String eps, String bps, String roe, String debtRatio) {
    return new StockFinancialSnapshot(
        code,
        "Stock " + code,
        LocalDate.of(2026, 3, 31),
        "Q1",
        new BigDecimal(eps),
        new BigDecimal(bps),
        new BigDecimal("100000000.00"),
        new BigDecimal("10000000.00"),
        new BigDecimal(roe),
        new BigDecimal(debtRatio)
    );
  }

  private class StaticPeerGroupService extends PeerGroupService {

    private final PeerGroup group;

    StaticPeerGroupService(PeerGroup group) {
      super(new com.jmens.advisor.modules.advisor.config.PeerGroupProperties(java.util.Map.of()));
      this.group = group;
    }

    @Override
    public PeerGroup findGroup(StockSymbol symbol) {
      return group;
    }
  }

  private class StubStockDataPort implements StockDataPort {

    @Override
    public StockQuote getRealtimeQuote(StockSymbol symbol) {
      return switch (symbol.code()) {
        case "000858" -> quote(symbol.code(), "120.00");
        case "000568" -> quote(symbol.code(), "80.00");
        case "600809" -> quote(symbol.code(), "90.00");
        default -> quote(symbol.code(), "100.00");
      };
    }

    @Override
    public List<com.jmens.advisor.modules.stock.domain.KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
      return List.of();
    }
  }

  private class StubStockFinancialPort implements StockFinancialPort {

    @Override
    public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
      return switch (symbol.code()) {
        case "000858" -> financial(symbol.code(), "4.00", "30.00", "18.00", "20.00");
        case "000568" -> financial(symbol.code(), "3.00", "20.00", "16.00", "25.00");
        case "600809" -> financial(symbol.code(), "5.00", "22.50", "22.00", "18.00");
        default -> financial(symbol.code(), "1.00", "10.00", "10.00", "10.00");
      };
    }
  }
}
