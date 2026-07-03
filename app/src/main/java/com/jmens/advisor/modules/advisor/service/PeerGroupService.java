package com.jmens.advisor.modules.advisor.service;

import com.jmens.advisor.modules.advisor.config.PeerGroupProperties;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeerGroupService {

  private final PeerGroupProperties properties;

  public PeerGroupService(PeerGroupProperties properties) {
    this.properties = properties;
  }

  public PeerGroup findGroup(StockSymbol symbol) {
    for (Map.Entry<String, List<String>> entry : properties.groups().entrySet()) {
      List<String> codes = entry.getValue();
      if (codes.contains(symbol.code())) {
        List<String> peers = codes.stream()
            .filter(code -> !code.equals(symbol.code()))
            .toList();
        return new PeerGroup(true, entry.getKey(), peers);
      }
    }
    return new PeerGroup(false, "", List.of());
  }

  public record PeerGroup(boolean available, String name, List<String> peerCodes) {}
}
