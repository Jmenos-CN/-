package com.jmens.advisor.modules.advisor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.advisor.config.PeerGroupProperties;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PeerGroupServiceTest {

  @Test
  void resolvesConfiguredPeerGroupByStockCode() {
    PeerGroupService service = new PeerGroupService(new PeerGroupProperties(Map.of(
        "liquor", List.of("600519", "000858", "000568", "600809")
    )));

    PeerGroupService.PeerGroup group = service.findGroup(new StockSymbol("600519", "SH"));

    assertThat(group.available()).isTrue();
    assertThat(group.name()).isEqualTo("liquor");
    assertThat(group.peerCodes()).containsExactly("000858", "000568", "600809");
  }

  @Test
  void returnsUnavailableGroupForUnknownStock() {
    PeerGroupService service = new PeerGroupService(new PeerGroupProperties(Map.of(
        "bank", List.of("600036", "601166")
    )));

    PeerGroupService.PeerGroup group = service.findGroup(new StockSymbol("600519", "SH"));

    assertThat(group.available()).isFalse();
    assertThat(group.peerCodes()).isEmpty();
  }
}
